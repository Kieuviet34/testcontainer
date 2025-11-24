import React, { useState, useEffect, useRef } from "react";
import Editor from "@monaco-editor/react";
import axios from "axios";
import "./style.css";

const API_BASE = process.env.REACT_APP_API_BASE || "http://localhost:8080/api";

const templates = {
  python: `print("Hello from Python")`,
  java: `public class Main {
    public static void main(String[] args) {
        System.out.println("Hello from Java");
    }
}`,
  c: `#include <stdio.h>
int main() {
    printf("Hello from C\\n");
    return 0;
}`,
  cpp: `#include <iostream>
using namespace std;
int main() {
    cout << "Hello from C++" << endl;
    return 0;
}`,
};

export default function App() {
  const [language, setLanguage] = useState("python");
  const [code, setCode] = useState(templates.python);
  const [jobId, setJobId] = useState(null);
  const [status, setStatus] = useState(null);
  const [output, setOutput] = useState("");
  const [running, setRunning] = useState(false);
  const pollRef = useRef(null);

  useEffect(() => {
    // update code template when language changes (if user hasn't edited — simple behavior)
    setCode(templates[language] || "");
  }, [language]);

  useEffect(() => {
    return () => {
      if (pollRef.current) clearInterval(pollRef.current);
    };
  }, []);

  const run = async () => {
    setJobId(null);
    setStatus("PENDING");
    setOutput("");
    setRunning(true);

    try {
      const resp = await axios.post(`${API_BASE}/run`, {
        language,
        code,
        stdin: ""
      });
      const id = resp.data?.jobId ?? resp.data?.JobId;
      setJobId(id);
      setStatus("PENDING");

      // start polling
      pollRef.current = setInterval(async () => {
        try {
          const r = await axios.get(`${API_BASE}/status/${id}`);
          const j = r.data;
          setStatus(j.status);
          if (j.output) setOutput(j.output);
          if (["DONE", "ERROR", "TIMEOUT"].includes(j.status)) {
            setRunning(false);
            clearInterval(pollRef.current);
            pollRef.current = null;
          }
        } catch (e) {
          console.error("poll error", e);
          // stop polling on 404 or network error after few tries? simple stop:
          // clearInterval(pollRef.current);
          // pollRef.current = null;
        }
      }, 1000);

    } catch (err) {
      setOutput("Run request failed: " + (err.message || err));
      setRunning(false);
      setStatus(null);
    }
  };

  const stopPolling = () => {
    if (pollRef.current) {
      clearInterval(pollRef.current);
      pollRef.current = null;
      setRunning(false);
    }
  };

  return (
    <div className="app">
      <header className="topbar">
        <h1>Code Runner — Monaco</h1>
        <div className="controls">
          <label>
            Language
            <select value={language} onChange={(e) => setLanguage(e.target.value)}>
              <option value="python">Python</option>
              <option value="java">Java</option>
              <option value="c">C</option>
              <option value="cpp">C++</option>
            </select>
          </label>
          <button className="run" onClick={run} disabled={running}>
            {running ? "Running..." : "Run"}
          </button>
          <button className="stop" onClick={stopPolling} disabled={!pollRef.current}>
            Stop Poll
          </button>
        </div>
      </header>

      <main className="main">
        <section className="editorWrap">
          <Editor
            height="60vh"
            defaultLanguage={language}
            language={language === "cpp" ? "cpp" : language}
            value={code}
            theme="vs-dark"
            onChange={(value) => setCode(value)}
            options={{
              fontSize: 14,
              minimap: { enabled: false },
              automaticLayout: true,
            }}
          />
        </section>

        <section className="outputWrap">
          <div className="meta">
            <div>Job: <b>{jobId || "-"}</b></div>
            <div>Status: <b>{status || "-"}</b></div>
          </div>
          <div className="outputBox">
            <pre>{output || (status ? "(no output yet)" : "Press Run to start")}</pre>
          </div>
        </section>
      </main>
    </div>
  );
}
