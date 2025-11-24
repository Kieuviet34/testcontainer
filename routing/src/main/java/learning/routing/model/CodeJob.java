package learning.routing.model;

public class CodeJob {
    public String id;
    public String language;
    public String code;
    public String stdin;
    public String status;
    public String output;
    public int exitCode;

    public CodeJob() {
    }

    public CodeJob(String id, String language, String code, String stdin) {
        this.id = id;
        this.language = language;
        this.code = code;
        this.stdin = stdin;
        this.status = "PENDING";
        this.output = "";
        this.exitCode = -1;
    }

    // --- Optional factory (nếu muốn dùng gọn) ---
    public static CodeJob create(String id, String language, String code, String stdin) {
        return new CodeJob(id, language, code, stdin);
    }

    // --- Getters & setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getStdin() { return stdin; }
    public void setStdin(String stdin) { this.stdin = stdin; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getOutput() { return output; }
    public void setOutput(String output) { this.output = output; }

    public int getExitCode() { return exitCode; }
    public void setExitCode(int exitCode) { this.exitCode = exitCode; }
}
