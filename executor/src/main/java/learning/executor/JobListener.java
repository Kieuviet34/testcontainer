package learning.executor;

import learning.executor.config.RunnerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;

@Component
public class JobListener {
    private static final Logger LOG = LoggerFactory.getLogger(JobListener.class);

    private final RestTemplate rest;
    private final String apiCallBackBase;

    public JobListener(RestTemplate restTemplate) {
        this.rest = restTemplate;
        this.apiCallBackBase = System.getenv().getOrDefault("API_URL", "http://localhost:8080");
    }
    private String callbackUrl() {
        String base = this.apiCallBackBase;
        if (base.endsWith("/")) base = base.substring(0, base.length()-1);
        if (base.endsWith("/api/callback")) return base;
        return base + "/api/callback";
    }

    @RabbitListener(queues = RunnerConfig.QUEUE)
    public void receive(CodeJob job) {
        LOG.info("Received Job: {} lang={}", job.getId(), job.getLanguage());
        job.setStatus("RUNNING");

        Path tmpDir = null;
        try {
            tmpDir = Files.createTempDirectory("codejob-" + job.getId());
            String filename = chooseFileName(job.getLanguage());
            Path source = tmpDir.resolve(filename);
            Files.writeString(source, job.getCode() == null ? "" : job.getCode());

            ProcessBuilder pb = buildDockerCommand(job.getLanguage(), tmpDir, filename);
            pb.redirectErrorStream(true);

            ExecutionResult res = runDockerProcess(pb, job.getStdin(), 15); // 15s timeout per job

            job.setOutput(res.output);
            job.setExitCode(res.exitCode);
            job.setStatus(determineStatusString(res.exitCode, res.timedOut));

            LOG.info("Job {} finished status={} exitCode={}", job.getId(), job.getStatus(), job.getExitCode());

        } catch (Exception ex) {
            LOG.error("Executor error for job {}: {}", job.getId(), ex.toString(), ex);
            job.setStatus("ERROR");
            job.setOutput("Executor error: " + ex.getMessage());
            job.setExitCode(-2);
        } finally {
            if (tmpDir != null) {
                try {
                    Files.walk(tmpDir)
                            .sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);
                } catch (Exception ignore) {
                    LOG.warn("Failed to cleanup temp dir for job {}", job.getId(), ignore);
                }
            }
        }

        // callback to API
        try {
            LOG.debug("POST -> {}", callbackUrl());
            rest.postForObject(callbackUrl(), job, Object.class);
        } catch (HttpClientErrorException he) {
            LOG.error("Callback HTTP error: status={} body={}", he.getStatusCode(), he.getResponseBodyAsString(), he);
        } catch (Exception e) {
            LOG.error("Failed to post callback for job {}: {}", job.getId(), e.getMessage(), e);
        }
    }

    private String determineStatusString(int exitCode, boolean timedOut) {
        if (timedOut) return "TIMEOUT";
        return exitCode == 0 ? "DONE" : "ERROR";
    }

    private String chooseFileName(String language) {
        if (language == null) return "main.py";
        switch (language.toLowerCase()) {
            case "java":
                return "Main.java";
            case "c":
                return "main.c";
            case "cpp":
            case "c++":
                return "main.cpp";
            case "py":
            case "python":
                return "main.py";
            default:
                return "main.py";
        }
    }

    /**
     * Build docker run command
     */
    private ProcessBuilder buildDockerCommand(String language, Path mountDir, String filename) {
        String mountPath = toDockerMountPath(mountDir);
        String mountArg = mountPath + ":/sandbox";

        String lang = (language == null) ? "python" : language.toLowerCase();
        String image;
        String payload;

        switch (lang) {
            case "java":
                image = "coderunner/java:17";
                // compile then run (stdout+stderr)
                payload = "javac " + filename + " 2>&1; echo JAVAC_EXIT:$?; if ls /sandbox/*.class >/dev/null 2>&1; then java -cp /sandbox Main 2>&1; else echo NO_CLASS; exit 127; fi";
                break;

            case "c":
                image = "coderunner/gcc:12";
                payload = "gcc -std=c11 -O2 -Wall -Wextra " + filename + " -o /sandbox/app 2>&1; echo GCC_EXIT:$?; if [ -f /sandbox/app ]; then timeout 10 /sandbox/app 2>&1; else echo NO_APP; exit 127; fi";
                break;

            case "cpp":
            case "c++":
                image = "coderunner/gcc:12";
                payload = "g++ -std=c++17 -O2 -Wall -Wextra " + filename + " -o /sandbox/app 2>&1; echo GPP_EXIT:$?; if [ -f /sandbox/app ]; then timeout 10 /sandbox/app 2>&1; else echo NO_APP; exit 127; fi";
                break;

            default:
                image = "coderunner/python:3.11";
                payload = "python /sandbox/" + filename + " 2>&1";
                break;
        }

        // build image if missing (non-fatal)
        try {
            autoBuildImageIfMissing(image, lang);
        } catch (Exception e) {
            LOG.warn("autoBuildImageIfMissing failed for {}: {}", image, e.getMessage());
        }

        List<String> cmd = new ArrayList<>();
        cmd.add("docker");
        cmd.add("run");
        cmd.add("-i");
        cmd.add("--rm");

        // isolation & resource flags
        cmd.add("--network");       cmd.add("none");
        cmd.add("--security-opt");  cmd.add("no-new-privileges:true");
        cmd.add("--cap-drop");      cmd.add("ALL");
        cmd.add("--read-only");
        cmd.add("--tmpfs");         cmd.add("/tmp:rw,size=64m");
        cmd.add("--memory");        cmd.add("128m");
        cmd.add("--cpus");          cmd.add("0.5");
        cmd.add("--pids-limit");    cmd.add("64");
        cmd.add("--user");          cmd.add("1000:1000");

        // mount workspace
        cmd.add("-v");              cmd.add(mountArg);
        cmd.add("-w");              cmd.add("/sandbox");

        // image + shell payload (payload passed as a single arg; no extra quotes)
        cmd.add(image);
        cmd.add(payload);

        LOG.info("FINAL DOCKER CMD TOKENS: {}", cmd);
        LOG.info("FINAL DOCKER CMD (readable): {}", String.join(" ", cmd));
        return new ProcessBuilder(cmd);
    }




    private String toDockerMountPath(Path p) {
        String path = p.toAbsolutePath().toString();
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            // Convert backslashes and C: style drive to /c/...
            path = path.replace('\\', '/');
            if (path.length() > 1 && path.charAt(1) == ':') {
                char drive = Character.toLowerCase(path.charAt(0));
                path = "/" + drive + path.substring(2);
            }
        }
        return path;
    }

    private ExecutionResult runDockerProcess(ProcessBuilder pb, String stdin, long timeoutSeconds) throws Exception {
        LOG.info("Docker command: {}", String.join(" ", pb.command()));
        Process proc = pb.start();

        // handle stdin
        if (stdin != null && !stdin.isEmpty()) {
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(proc.getOutputStream()))) {
                writer.write(stdin);
                writer.flush();
            } catch (IOException e) {
                LOG.warn("Failed to write stdin to process: {}", e.getMessage());
            }
        } else {
            // close stdin to signal EOF
            try {
                proc.getOutputStream().close();
            } catch (IOException ignored) {}
        }

        ExecutorService readerPool = Executors.newSingleThreadExecutor();
        Future<String> outputFuture = readerPool.submit(() -> {
            try (InputStream is = proc.getInputStream();
                 BufferedReader r = new BufferedReader(new InputStreamReader(is))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                return sb.toString();
            }
        });

        boolean finished = proc.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        String output;
        int exitCode;
        boolean timedOut = false;

        if (!finished) {
            timedOut = true;
            proc.destroyForcibly();
            exitCode = -1;
            output = "TIMEOUT: process killed after " + timeoutSeconds + "s";
        } else {
            exitCode = proc.exitValue();
            try {
                output = outputFuture.get(2, TimeUnit.SECONDS);
            } catch (Exception e) {
                output = "Failed to read output: " + e.getMessage();
            }
        }

        readerPool.shutdownNow();
        return new ExecutionResult(exitCode, output, timedOut);
    }

    private static class ExecutionResult {
        final int exitCode;
        final String output;
        final boolean timedOut;

        ExecutionResult(int exitCode, String output, boolean timedOut) {
            this.exitCode = exitCode;
            this.output = output;
            this.timedOut = timedOut;
        }
    }
    private void autoBuildImageIfMissing(String image, String lang) {
        try {
            ProcessBuilder pb = new ProcessBuilder("docker", "image", "inspect", image);
            Process p = pb.start();
            if (p.waitFor(10, TimeUnit.SECONDS) && p.exitValue() == 0) {
                LOG.debug("Image {} đã có sẵn.", image);
                return;
            }
        } catch (Exception e) {
            LOG.warn("Image {} chưa tồn tại, sẽ build...", image);
        }

        String langDir = switch (lang) {
            case "java" -> "java";
            case "c", "cpp", "c++" -> "gcc";
            default -> "python";
        };

        String projectRoot = System.getProperty("user.dir");
        String contextPath = projectRoot + File.separator + "executor" + File.separator + "images" + File.separator + langDir;

        LOG.info("Đang build image {} từ {}", image, contextPath);

        ProcessBuilder buildPb = new ProcessBuilder(
                "docker", "build", "-f", contextPath + File.separator + "Dockerfile", "-t", image, contextPath
        );
        buildPb.redirectErrorStream(true);
        try {
            Process buildProc = buildPb.start();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(buildProc.getInputStream()))) {
                br.lines().forEach(line -> LOG.info("[build:{}] {}", langDir, line));
            }
            boolean finished = buildProc.waitFor(900, TimeUnit.SECONDS);
            if (!finished || buildProc.exitValue() != 0) {
                LOG.error("BUILD IMAGE {} THẤT BẠI! Job sẽ lỗi.", image);
            } else {
                LOG.info("BUILD IMAGE {} THÀNH CÔNG!", image);
            }
        } catch (Exception ex) {
            LOG.error("Lỗi khi build image {}", image, ex);
        }
    }
}
