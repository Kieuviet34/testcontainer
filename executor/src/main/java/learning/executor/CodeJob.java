package learning.executor;

public class CodeJob {
    public String id;
    public String language;
    public String code;
    public String Stdin;
    public String Status;
    public String output;
    public int exitCode;

    public CodeJob() {
    }

    public CodeJob(String id, String language, String code, String stdin) {
        this.id = id;
        this.language = language;
        this.code = code;
        this.Stdin = stdin;
        this.Status = "PENDING";
        this.output = "";
        this.exitCode = -1;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getStdin() {
        return Stdin;
    }

    public void setStdin(String stdin) {
        Stdin = stdin;
    }

    public String getStatus() {
        return Status;
    }

    public void setStatus(String status) {
        Status = status;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public int getExitCode() {
        return exitCode;
    }

    public void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    }
}
