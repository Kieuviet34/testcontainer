package learning.executor;

import java.io.Serializable;

public class CodeJob implements Serializable {
    private String id;
    private String language;
    private String code;
    private String stdin;

    // Các trường update trạng thái
    private String status;
    private String output;
    private int exitCode;

    public CodeJob() {
    }

    public CodeJob(String id, String language, String code, String stdin, String status, String output, int exitCode) {
        this.id = id;
        this.language = language;
        this.code = code;
        this.stdin = stdin;
        this.status = status;
        this.output = output;
        this.exitCode = exitCode;
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
        return stdin;
    }

    public void setStdin(String stdin) {
        this.stdin = stdin;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
