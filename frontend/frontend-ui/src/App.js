import React, { useState, useEffect, useRef } from "react";
import Editor from "@monaco-editor/react";
import axios from "axios";
import "./style.css"; // Nhớ import file css mới

const API_BASE = process.env.REACT_APP_API_BASE || "http://localhost:8080/api";

const templates = {
    python: `import sys\n\n# Nhập input vào tab STDIN bên dưới\n# data = sys.stdin.read()\nprint("Hello SmartCode!")`,
    java: `import java.util.Scanner;\n\npublic class Main {\n    public static void main(String[] args) {\n        System.out.println("Hello SmartCode Java!");\n        // Scanner scanner = new Scanner(System.in);\n    }\n}`,
    c: `#include <stdio.h>\n\nint main() {\n    printf("Hello SmartCode C!\\n");\n    return 0;\n}`,
    cpp: `#include <iostream>\nusing namespace std;\n\nint main() {\n    cout << "Hello SmartCode C++!" << endl;\n    return 0;\n}`,
};

export default function App() {
    const [language, setLanguage] = useState("python");
    const [code, setCode] = useState(templates.python);
    const [inputData, setInputData] = useState("");
    const [output, setOutput] = useState("");
    const [status, setStatus] = useState("IDLE");
    const [running, setRunning] = useState(false);
    const [activeTab, setActiveTab] = useState("input"); // 'input' | 'console'

    const pollRef = useRef(null);

    useEffect(() => {
        // Chỉ reset code nếu người dùng chuyển ngôn ngữ (thực tế nên check xem code có bị sửa chưa)
        setCode(templates[language] || "");
    }, [language]);

    useEffect(() => {
        return () => stopPolling();
    }, []);

    const run = async () => {
        setStatus("PENDING");
        setOutput("");
        setRunning(true);
        setActiveTab("console"); // Auto switch to console

        try {
            const payload = {
                lang: language,
                typed_code: code,
                data_input: inputData
            };

            const resp = await axios.post(`${API_BASE}/intepret_solution`, payload);
            const id = resp.data?.interpret_id;

            if (!id) throw new Error("Server error: No ID returned");

            pollRef.current = setInterval(async () => {
                try {
                    const r = await axios.get(`${API_BASE}/check/${id}`);
                    const data = r.data;

                    if (data.state === "PENDING" || data.state === "RUNNING") {
                        setOutput((prev) => prev === "Running..." ? "Running." : "Running...");
                    } else {
                        setRunning(false);
                        clearInterval(pollRef.current);
                        pollRef.current = null;

                        const finalStatus = data.status || "DONE";
                        setStatus(finalStatus);

                        let rawOutput = data.output || "";
                        if (!rawOutput && finalStatus === "DONE") rawOutput = "(Program exited with no output)";

                        setOutput(rawOutput);
                    }
                } catch (e) {
                    console.error(e);
                }
            }, 1000);

        } catch (err) {
            setOutput("System Error: " + (err.response?.data?.error || err.message));
            setRunning(false);
            setStatus("ERROR");
            stopPolling();
        }
    };

    const stopPolling = () => {
        if (pollRef.current) {
            clearInterval(pollRef.current);
            pollRef.current = null;
        }
        setRunning(false);
    };

    return (
        <div className="app">
            {/* --- HEADER --- */}
            <header className="topbar">
                <div className="brand">
                    <span>⚡</span> SmartCode Console
                </div>
                <div className="controls">
                    <select
                        className="lang-select"
                        value={language}
                        onChange={(e) => setLanguage(e.target.value)}
                        disabled={running}
                    >
                        <option value="python">Python 3</option>
                        <option value="java">Java 17</option>
                        <option value="c">C (GCC 12)</option>
                        <option value="cpp">C++ (G++ 17)</option>
                    </select>

                    <button className="btn-run" onClick={run} disabled={running}>
                        {running ? "Running..." : "▶ Run Code"}
                    </button>

                    {running && (
                        <button className="btn-stop" onClick={stopPolling}>Stop</button>
                    )}
                </div>
            </header>

            {/* --- BODY --- */}
            <div className="main">
                {/* 1. Editor Section (Chiếm phần trên) */}
                <section className="editorWrap">
                    <Editor
                        height="100%" // Bắt buộc để Monaco fill hết cha
                        width="100%"
                        language={language === "cpp" ? "cpp" : language}
                        value={code}
                        theme="vs-dark"
                        onChange={(val) => setCode(val)}
                        options={{
                            fontSize: 14,
                            minimap: { enabled: false },
                            scrollBeyondLastLine: false,
                            automaticLayout: true,
                            fontFamily: "'Consolas', 'Courier New', monospace"
                        }}
                    />
                </section>

                {/* 2. Console/Input Section (Chiếm phần dưới cố định) */}
                <section className="bottomPanel">
                    <div className="tabs">
                        <div
                            className={`tab ${activeTab === "input" ? "active" : ""}`}
                            onClick={() => setActiveTab("input")}
                        >
                            STDIN (Input)
                        </div>
                        <div
                            className={`tab ${activeTab === "console" ? "active" : ""}`}
                            onClick={() => setActiveTab("console")}
                        >
                            Console Output
                        </div>
                    </div>

                    <div className="panelContent">
                        {activeTab === "input" && (
                            <textarea
                                className="inputArea"
                                placeholder="Nhập dữ liệu đầu vào cho chương trình (nếu có)..."
                                value={inputData}
                                onChange={(e) => setInputData(e.target.value)}
                                spellCheck={false}
                            />
                        )}

                        {activeTab === "console" && (
                            <div className={`terminal ${status === "ERROR" ? "terminal-error" : "terminal-success"}`}>
                                {output}
                                {status !== "PENDING" && status !== "IDLE" && (
                                    <div className="status-line">
                                        Process finished with status: <b>{status}</b>
                                    </div>
                                )}
                            </div>
                        )}
                    </div>
                </section>
            </div>
        </div>
    );
}