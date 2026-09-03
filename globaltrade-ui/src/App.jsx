import { useState } from "react";
import "./App.css";
import Login      from "./Login";
import Dashboard  from "./pages/Dashboard";
import Search     from "./pages/Search";
import Inventory  from "./pages/Inventory";

/**
 * App.jsx — root component.
 *
 * After login, we have:
 *  - credentials: { username, password }
 *  - me:          { username, roles: ["ADMIN", ...] }
 *
 * The sidebar nav items shown depend on the user's roles:
 *  - Dashboard:  all roles
 *  - Search:     all roles
 *  - Inventory:  ADMIN, LOGISTICS_COORDINATOR, WAREHOUSE_MANAGER only
 */
export default function App() {
  const [credentials, setCredentials] = useState(null);
  const [me, setMe]                   = useState(null);
  const [page, setPage]               = useState("dashboard");

  function handleLogin(creds, meData) {
    setCredentials(creds);
    setMe(meData);
  }

  function handleLogout() {
    setCredentials(null);
    setMe(null);
    setPage("dashboard");
  }

  if (!credentials) return <Login onLogin={handleLogin} />;

  const roles = me?.roles ?? [];
  const hasRole = (...r) => r.some(role => roles.includes(role));

  // Build nav based on roles
  const navItems = [
    { id: "dashboard",  label: "Dashboard",  icon: "📊", show: true },
    { id: "inventory",  label: "Inventory",  icon: "📦", show: hasRole("ADMIN","LOGISTICS_COORDINATOR","WAREHOUSE_MANAGER") },
    { id: "search",     label: "Search",     icon: "🔍", show: true },
  ].filter(n => n.show);

  // Role badge colour
  const roleColors = {
    ADMIN:                "#f59e0b",
    LOGISTICS_COORDINATOR:"#3b82f6",
    WAREHOUSE_MANAGER:    "#10b981",
    VENDOR_REPRESENTATIVE:"#8b5cf6",
    CUSTOMS_AGENT:        "#ec4899",
  };
  const primaryRole  = roles[0] ?? "UNKNOWN";
  const roleColor    = roleColors[primaryRole] ?? "#94a3b8";
  const roleLabel    = primaryRole.replace(/_/g," ");

  return (
    <div className="layout">
      <aside className="sidebar">
        <div className="sidebar-logo">🌐 GlobalTrade</div>
        <div className="sidebar-sub">Supply Chain Management</div>

        {navItems.map(n => (
          <button key={n.id} className={`nav-item ${page === n.id ? "active" : ""}`}
                  onClick={() => setPage(n.id)}>
            {n.icon} {n.label}
          </button>
        ))}

        <div className="nav-spacer" />

        {/* Logged-in user info */}
        <div style={{ padding:"0.6rem 0.75rem", background:"var(--surface2)", borderRadius:8, marginBottom:"0.5rem" }}>
          <div style={{ fontSize:"0.78rem", color:"var(--text-muted)" }}>Signed in as</div>
          <div style={{ fontWeight:600, fontSize:"0.88rem" }}>{me?.username}</div>
          <div style={{ fontSize:"0.7rem", color: roleColor, marginTop:"0.2rem", fontWeight:600 }}>
            {roleLabel}
          </div>
        </div>

        <button className="logout-btn" onClick={handleLogout}>🚪 Sign Out</button>
      </aside>

      <div className="main">
        <header className="topbar">
          <h2>
            {navItems.find(n => n.id === page)?.icon}{" "}
            {navItems.find(n => n.id === page)?.label}
          </h2>
          <div className="topbar-right">
            <span style={{ fontSize:"0.78rem", padding:"0.2rem 0.6rem", background:"var(--surface2)", borderRadius:99, color: roleColor, fontWeight:600 }}>
              {roleLabel}
            </span>
            <span style={{ opacity:0.4 }}>|</span>
            <span>Payara 6 · Jakarta EE 10</span>
          </div>
        </header>

        {page === "dashboard" && <Dashboard credentials={credentials} me={me} />}
        {page === "inventory" && <Inventory credentials={credentials} me={me} />}
        {page === "search"    && <Search    credentials={credentials} me={me} />}
      </div>
    </div>
  );
}
