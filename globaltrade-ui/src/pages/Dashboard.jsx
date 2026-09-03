import { useState, useEffect } from "react";
import {
  getAllShipments, getShipmentsByStatus,
  createShipment, updateShipmentStatus,
  clearCustoms, cancelShipment
} from "../api";

const ALL_STATUSES = ["PENDING","IN_TRANSIT","CUSTOMS_HOLD","DELAYED","DELIVERED","CANCELLED"];

export default function Dashboard({ credentials, me }) {
  const [shipments, setShipments]   = useState([]);
  const [loading, setLoading]       = useState(true);
  const [error, setError]           = useState("");
  const [statusFilter, setStatus]   = useState("ALL");
  const [showCreate, setShowCreate] = useState(false);
  const [actionRow, setActionRow]   = useState(null); // shipment being acted on

  const roles = me?.roles ?? [];
  const can = (role) => roles.includes(role);
  const isAdmin      = can("ADMIN");
  const isLogistics  = can("LOGISTICS_COORDINATOR") || isAdmin;
  const isWarehouse  = can("WAREHOUSE_MANAGER") || isAdmin;
  const isCustoms    = can("CUSTOMS_AGENT") || isAdmin;
  const canCreate    = isLogistics;
  const canUpdate    = isLogistics || isWarehouse || can("VENDOR_REPRESENTATIVE");
  const canCancel    = isAdmin;
  const canCustoms   = isCustoms;

  useEffect(() => { fetchShipments(); }, [statusFilter]);

  async function fetchShipments() {
    setLoading(true); setError("");
    try {
      const data = statusFilter === "ALL"
        ? await getAllShipments(credentials)
        : await getShipmentsByStatus(credentials, statusFilter);
      setShipments(data);
    } catch (e) { setError(e.message); }
    finally { setLoading(false); }
  }

  async function handleStatusUpdate(shipment, newStatus) {
    try {
      await updateShipmentStatus(credentials, shipment.id, newStatus);
      await fetchShipments();
      setActionRow(null);
    } catch(e) { alert("Error: " + e.message); }
  }

  async function handleCustoms(shipment) {
    if (!window.confirm(`Clear customs for shipment ${shipment.trackingNumber}?`)) return;
    try {
      await clearCustoms(credentials, shipment.id);
      await fetchShipments();
    } catch(e) { alert("Error: " + e.message); }
  }

  async function handleCancel(shipment) {
    if (!window.confirm(`Cancel shipment ${shipment.trackingNumber}? This cannot be undone.`)) return;
    try {
      await cancelShipment(credentials, shipment.id);
      await fetchShipments();
    } catch(e) { alert("Error: " + e.message); }
  }

  const total       = shipments.length;
  const inTransit   = shipments.filter(s => s.status === "IN_TRANSIT").length;
  const delayed     = shipments.filter(s => s.status === "DELAYED").length;
  const customs     = shipments.filter(s => s.status === "CUSTOMS_HOLD").length;
  const delivered   = shipments.filter(s => s.status === "DELIVERED").length;

  return (
    <div className="page">
      {/* Stats */}
      <div className="stats-grid">
        <StatCard label="Total"        value={total}     color="#3b82f6" />
        <StatCard label="In Transit"   value={inTransit} color="#0ea5e9" />
        <StatCard label="Delayed"      value={delayed}   color="#dc2626" />
        <StatCard label="Customs Hold" value={customs}   color="#d97706" />
        <StatCard label="Delivered"    value={delivered} color="#16a34a" />
      </div>

      {/* Table card */}
      <div className="table-card">
        <div className="table-header">
          <h3>Shipments</h3>
          <div className="table-controls">
            <select className="filter-select" value={statusFilter} onChange={e => setStatus(e.target.value)}>
              <option value="ALL">All Statuses</option>
              {ALL_STATUSES.map(s => <option key={s} value={s}>{s.replace("_"," ")}</option>)}
            </select>
            {canCreate && (
              <button className="btn-primary" onClick={() => setShowCreate(true)}>+ New Shipment</button>
            )}
            <button className="btn-secondary" onClick={fetchShipments}>↻ Refresh</button>
          </div>
        </div>

        {loading && <div className="state-box"><div className="spinner"/><p>Loading…</p></div>}
        {error   && <div className="state-box"><p style={{color:"var(--danger)"}}>⚠ {error}</p></div>}
        {!loading && !error && shipments.length === 0 && (
          <div className="state-box">
            <p>No shipments found.</p>
            {canCreate && <button className="btn-primary" style={{marginTop:"0.75rem"}} onClick={() => setShowCreate(true)}>Create first shipment</button>}
          </div>
        )}

        {!loading && !error && shipments.length > 0 && (
          <div className="table-scroll-wrapper">
            <table>
              <thead>
                <tr>
                  <th>ID</th><th>Tracking #</th><th>Origin</th>
                  <th>Destination</th><th>Status</th><th>Carrier</th>
                  <th>Customs</th>
                  {(canUpdate || canCancel || canCustoms) && <th>Actions</th>}
                </tr>
              </thead>
              <tbody>
                {shipments.map(s => (
                  <tr key={s.id}>
                    <td style={{color:"var(--text-muted)",fontSize:"0.8rem"}}>#{s.id}</td>
                    <td style={{fontFamily:"monospace",fontSize:"0.82rem"}}>{s.trackingNumber}</td>
                    <td>{s.origin}</td>
                    <td>{s.destination}</td>
                    <td><span className={`badge badge-${s.status}`}>{s.status.replace("_"," ")}</span></td>
                    <td>{s.carrier || <span style={{color:"var(--text-muted)"}}>—</span>}</td>
                    <td>{s.customsCleared ? <span className="customs-yes">✓ Cleared</span> : <span className="customs-no">Pending</span>}</td>
                    {(canUpdate || canCancel || canCustoms) && (
                      <td>
                        <div style={{display:"flex",gap:"0.4rem",flexWrap:"wrap"}}>
                          {canUpdate && s.status !== "CANCELLED" && s.status !== "DELIVERED" && (
                            <button className="btn-secondary" style={{fontSize:"0.75rem",padding:"0.3rem 0.6rem"}}
                              onClick={() => setActionRow(actionRow?.id === s.id ? null : s)}>
                              ✏ Status
                            </button>
                          )}
                          {canCustoms && s.status === "CUSTOMS_HOLD" && !s.customsCleared && (
                            <button className="btn-secondary" style={{fontSize:"0.75rem",padding:"0.3rem 0.6rem",color:"var(--success)"}}
                              onClick={() => handleCustoms(s)}>
                              🛃 Clear
                            </button>
                          )}
                          {canCancel && s.status !== "CANCELLED" && (
                            <button className="btn-secondary" style={{fontSize:"0.75rem",padding:"0.3rem 0.6rem",color:"var(--danger)"}}
                              onClick={() => handleCancel(s)}>
                              ✕ Cancel
                            </button>
                          )}
                        </div>
                        {/* Inline status picker */}
                        {actionRow?.id === s.id && (
                          <div style={{marginTop:"0.4rem",display:"flex",gap:"0.3rem",flexWrap:"wrap"}}>
                            {ALL_STATUSES.filter(st => st !== s.status && st !== "CANCELLED").map(st => (
                              <button key={st} className="btn-primary"
                                style={{fontSize:"0.7rem",padding:"0.25rem 0.5rem"}}
                                onClick={() => handleStatusUpdate(s, st)}>
                                {st.replace("_"," ")}
                              </button>
                            ))}
                          </div>
                        )}
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {showCreate && (
        <CreateModal credentials={credentials} onClose={() => setShowCreate(false)}
                     onCreated={() => { setShowCreate(false); fetchShipments(); }} />
      )}
    </div>
  );
}

function StatCard({ label, value, color }) {
  return (
    <div className="stat-card">
      <div className="stat-label">{label}</div>
      <div className="stat-value" style={{color}}>{value}</div>
    </div>
  );
}

function CreateModal({ credentials, onClose, onCreated }) {
  const [form, setForm]   = useState({ trackingNumber:"", originCountry:"", destinationCountry:"", carrierName:"" });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  async function handleSubmit(e) {
    e.preventDefault(); setLoading(true); setError("");
    try {
      await createShipment(credentials, form);
      onCreated();
    } catch(e) { setError(e.message); }
    finally { setLoading(false); }
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={e => e.stopPropagation()}>
        <h3>Create New Shipment</h3>
        <form onSubmit={handleSubmit}>
          {[
            ["trackingNumber","Tracking Number","GT-2024-00001"],
            ["originCountry","Origin Country","Sri Lanka"],
            ["destinationCountry","Destination Country","UAE"],
            ["carrierName","Carrier Name","Emirates SkyCargo"],
          ].map(([name,label,ph]) => (
            <div className="form-row" key={name}>
              <label>{label}</label>
              <input name={name} value={form[name]} placeholder={ph}
                     required={name !== "carrierName"}
                     onChange={e => setForm(f => ({...f,[name]:e.target.value}))} />
            </div>
          ))}
          {error && <p style={{color:"var(--danger)",fontSize:"0.82rem",marginTop:"0.5rem"}}>{error}</p>}
          <div className="modal-actions">
            <button type="button" className="btn-secondary" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn-primary" disabled={loading}>{loading ? "Creating…" : "Create"}</button>
          </div>
        </form>
      </div>
    </div>
  );
}
