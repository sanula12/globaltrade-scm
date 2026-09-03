import { useState } from "react";
import { getMe } from "./api";

export default function Login({ onLogin }) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading]   = useState(false);
  const [error, setError]       = useState("");

  async function handleSubmit(e) {
    e.preventDefault();
    setLoading(true);
    setError("");

    const creds = { username, password };
    try {
      // /me validates credentials AND returns the user's roles in one call
      const me = await getMe(creds);
      onLogin(creds, me);
    } catch (err) {
      console.error("Login error:", err);
      if (err.status === 401 || err.status === 403) {
        setError("Wrong username or password.");
      } else if (err.message && err.message.includes("fetch")) {
        setError("CORS or network error — check browser console (F12).");
      } else {
        setError(`Error (${err.status ?? "network"}): ${err.message}`);
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-wrapper">
      <div className="auth-card">
        <div className="auth-logo">
          <h1>🌐 GlobalTrade SCM</h1>
          <p>Supply Chain Management System</p>
        </div>
        <form onSubmit={handleSubmit}>
          <label>Username</label>
          <input type="text" value={username} onChange={e => setUsername(e.target.value)}
                 placeholder="admin" autoFocus required />
          <label>Password</label>
          <input type="password" value={password} onChange={e => setPassword(e.target.value)}
                 placeholder="••••••••" required />
          <button className="btn-primary" type="submit" disabled={loading}>
            {loading ? "Signing in…" : "Sign In"}
          </button>
        </form>
        {error && <p className="auth-error">{error}</p>}

        <div style={{ marginTop: "1.5rem", padding: "0.75rem", background: "var(--bg)", borderRadius: 6, fontSize: "0.78rem", color: "var(--text-muted)" }}>
          <strong style={{ color: "var(--text)" }}>Demo accounts — password: <span style={{color:"var(--accent)"}}>Admin@1234</span></strong>
          <div style={{ marginTop: "0.4rem", display: "grid", gridTemplateColumns: "1fr 1fr", gap: "0.2rem 0.75rem" }}>
            <span>admin</span>       <span style={{color:"#f59e0b"}}>ADMIN</span>
            <span>coordinator</span> <span style={{color:"#3b82f6"}}>LOGISTICS</span>
            <span>warehouse</span>   <span style={{color:"#10b981"}}>WAREHOUSE</span>
            <span>vendor</span>      <span style={{color:"#8b5cf6"}}>VENDOR</span>
            <span>customs</span>     <span style={{color:"#ec4899"}}>CUSTOMS</span>
          </div>
        </div>
      </div>
    </div>
  );
}
