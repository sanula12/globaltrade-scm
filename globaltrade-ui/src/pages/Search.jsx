import { useState } from "react";

/**
 * Search page — look up a shipment by ID or tracking number.
 * Demonstrates: controlled inputs, conditional rendering, individual API calls.
 */
export default function Search({ credentials }) {
  const { username, password } = credentials;

  const [mode, setMode]       = useState("tracking"); // "tracking" | "id"
  const [query, setQuery]     = useState("");
  const [result, setResult]   = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError]     = useState("");

  async function handleSearch(e) {
    e.preventDefault();
    setLoading(true);
    setError("");
    setResult(null);

    const url =
      mode === "id"
        ? `http://localhost:8080/globaltrade/shipments?id=${encodeURIComponent(query)}`
        : `http://localhost:8080/globaltrade/shipments?tracking=${encodeURIComponent(query)}`;

    try {
      const res = await fetch(url, {
        headers: { Authorization: "Basic " + btoa(`${username}:${password}`) },
      });
      if (res.status === 404) { setError("Shipment not found."); return; }
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      setResult(await res.json());
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="page">
      <div className="table-card" style={{ maxWidth: 640 }}>
        <div className="table-header">
          <h3>Search Shipment</h3>
        </div>
        <div style={{ padding: "1.5rem" }}>
          {/* mode toggle */}
          <div style={{ display: "flex", gap: "0.5rem", marginBottom: "1rem" }}>
            <button
              className={mode === "tracking" ? "btn-primary" : "btn-secondary"}
              onClick={() => { setMode("tracking"); setQuery(""); setResult(null); setError(""); }}
            >By Tracking #</button>
            <button
              className={mode === "id" ? "btn-primary" : "btn-secondary"}
              onClick={() => { setMode("id"); setQuery(""); setResult(null); setError(""); }}
            >By ID</button>
          </div>

          <form onSubmit={handleSearch} style={{ display: "flex", gap: "0.75rem" }}>
            <input
              style={{ flex: 1, padding: "0.6rem 0.8rem", borderRadius: 6, border: "1px solid var(--border)", background: "var(--bg)", color: "var(--text)", fontSize: "0.9rem" }}
              value={query}
              onChange={e => setQuery(e.target.value)}
              placeholder={mode === "tracking" ? "GT-2024-00001" : "1"}
              required
            />
            <button className="btn-primary" type="submit" disabled={loading}>
              {loading ? "…" : "Search"}
            </button>
          </form>

          {error && <p style={{ color: "var(--danger)", marginTop: "1rem", fontSize: "0.85rem" }}>⚠ {error}</p>}

          {result && (
            <div style={{ marginTop: "1.5rem", background: "var(--bg)", borderRadius: 8, padding: "1.25rem", border: "1px solid var(--border)" }}>
              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "1rem" }}>
                <Field label="ID"              value={`#${result.id}`} />
                <Field label="Tracking Number" value={result.trackingNumber} mono />
                <Field label="Origin"          value={result.origin} />
                <Field label="Destination"     value={result.destination} />
                <Field label="Carrier"         value={result.carrier || "—"} />
                <Field label="Customs Cleared" value={result.customsCleared ? "✓ Yes" : "Pending"} />
                <div style={{ gridColumn: "1 / -1" }}>
                  <Field label="Status" value={<span className={`badge badge-${result.status}`}>{result.status}</span>} />
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function Field({ label, value, mono }) {
  return (
    <div>
      <div style={{ fontSize: "0.72rem", color: "var(--text-muted)", textTransform: "uppercase", letterSpacing: "0.05em", marginBottom: "0.25rem" }}>{label}</div>
      <div style={{ fontSize: "0.9rem", fontFamily: mono ? "monospace" : "inherit" }}>{value}</div>
    </div>
  );
}
