import { useState, useEffect } from "react";
import { getAllInventory, getLowStock, addInventory, restockInventory } from "../api";

export default function Inventory({ credentials, me }) {
  const roles = me?.roles ?? [];
  const isAdmin    = roles.includes("ADMIN");
  const isWarehouse= roles.includes("WAREHOUSE_MANAGER") || isAdmin;
  const canAdd     = isWarehouse;
  const canRestock = isWarehouse;

  const [items, setItems]       = useState([]);
  const [loading, setLoading]   = useState(true);
  const [error, setError]       = useState("");
  const [filter, setFilter]     = useState("ALL"); // ALL | LOW
  const [showAdd, setShowAdd]   = useState(false);
  const [restockRow, setRestockRow] = useState(null);
  const [restockQty, setRestockQty] = useState("");

  useEffect(() => { fetchItems(); }, [filter]);

  async function fetchItems() {
    setLoading(true); setError("");
    try {
      const data = filter === "LOW" ? await getLowStock(credentials) : await getAllInventory(credentials);
      setItems(data);
    } catch(e) { setError(e.message); }
    finally { setLoading(false); }
  }

  async function handleRestock(sku) {
    if (!restockQty || isNaN(restockQty)) { alert("Enter a valid quantity"); return; }
    try {
      await restockInventory(credentials, sku, restockQty);
      setRestockRow(null); setRestockQty("");
      fetchItems();
    } catch(e) { alert("Error: " + e.message); }
  }

  const lowCount = items.filter(i => i.isLowStock).length;

  return (
    <div className="page">
      <div className="stats-grid">
        <StatCard label="Total Items"  value={items.length} color="#3b82f6" />
        <StatCard label="Low Stock"    value={lowCount}     color="#dc2626" />
        <StatCard label="Healthy"      value={items.length - lowCount} color="#16a34a" />
      </div>

      <div className="table-card">
        <div className="table-header">
          <h3>Inventory</h3>
          <div className="table-controls">
            <select className="filter-select" value={filter} onChange={e => setFilter(e.target.value)}>
              <option value="ALL">All Items</option>
              <option value="LOW">Low Stock Only</option>
            </select>
            {canAdd && <button className="btn-primary" onClick={() => setShowAdd(true)}>+ Add Item</button>}
            <button className="btn-secondary" onClick={fetchItems}>↻ Refresh</button>
          </div>
        </div>

        {loading && <div className="state-box"><div className="spinner"/><p>Loading…</p></div>}
        {error   && <div className="state-box"><p style={{color:"var(--danger)"}}>⚠ {error}</p></div>}

        {!loading && !error && items.length === 0 && (
          <div className="state-box">
            <p>No inventory items found.</p>
            {canAdd && <button className="btn-primary" style={{marginTop:"0.75rem"}} onClick={() => setShowAdd(true)}>Add first item</button>}
          </div>
        )}

        {!loading && !error && items.length > 0 && (
          <div className="table-scroll-wrapper">
            <table>
              <thead>
                <tr>
                  <th>SKU</th><th>Product Name</th><th>Warehouse</th>
                  <th>Qty</th><th>Reorder At</th><th>Status</th>
                  {canRestock && <th>Actions</th>}
                </tr>
              </thead>
              <tbody>
                {items.map(item => (
                  <tr key={item.id}>
                    <td style={{fontFamily:"monospace",fontSize:"0.82rem"}}>{item.sku}</td>
                    <td>{item.productName}</td>
                    <td>{item.warehouse}</td>
                    <td style={{fontWeight:600, color: item.isLowStock ? "var(--danger)" : "var(--success)"}}>
                      {item.quantity}
                    </td>
                    <td style={{color:"var(--text-muted)"}}>{item.reorderThreshold}</td>
                    <td>
                      {item.isLowStock
                        ? <span className="badge" style={{background:"rgba(220,38,38,0.15)",color:"var(--danger)"}}>⚠ Low Stock</span>
                        : <span className="badge" style={{background:"rgba(22,163,74,0.15)",color:"var(--success)"}}>OK</span>}
                    </td>
                    {canRestock && (
                      <td>
                        {restockRow === item.sku ? (
                          <div style={{display:"flex",gap:"0.35rem",alignItems:"center"}}>
                            <input type="number" min="1" value={restockQty}
                              onChange={e => setRestockQty(e.target.value)}
                              placeholder="qty"
                              style={{width:60,padding:"0.25rem 0.4rem",borderRadius:4,border:"1px solid var(--border)",background:"var(--bg)",color:"var(--text)",fontSize:"0.8rem"}} />
                            <button className="btn-primary" style={{fontSize:"0.75rem",padding:"0.3rem 0.55rem"}}
                              onClick={() => handleRestock(item.sku)}>OK</button>
                            <button className="btn-secondary" style={{fontSize:"0.75rem",padding:"0.3rem 0.55rem"}}
                              onClick={() => setRestockRow(null)}>✕</button>
                          </div>
                        ) : (
                          <button className="btn-secondary" style={{fontSize:"0.75rem",padding:"0.3rem 0.6rem"}}
                            onClick={() => { setRestockRow(item.sku); setRestockQty(""); }}>
                            + Restock
                          </button>
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

      {showAdd && (
        <AddModal credentials={credentials} onClose={() => setShowAdd(false)}
                  onAdded={() => { setShowAdd(false); fetchItems(); }} />
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

function AddModal({ credentials, onClose, onAdded }) {
  const [form, setForm]   = useState({ sku:"", productName:"", warehouseLocation:"", quantity:"0", reorderThreshold:"10" });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  async function handleSubmit(e) {
    e.preventDefault(); setLoading(true); setError("");
    try {
      await addInventory(credentials, form);
      onAdded();
    } catch(e) { setError(e.message); }
    finally { setLoading(false); }
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={e => e.stopPropagation()}>
        <h3>Add Inventory Item</h3>
        <form onSubmit={handleSubmit}>
          {[
            ["sku","SKU","ELEC-001"],
            ["productName","Product Name","Laptop"],
            ["warehouseLocation","Warehouse Location","Dubai-WH1"],
            ["quantity","Initial Quantity","100"],
            ["reorderThreshold","Reorder Threshold","10"],
          ].map(([name,label,ph]) => (
            <div className="form-row" key={name}>
              <label>{label}</label>
              <input name={name} value={form[name]} placeholder={ph} required
                     onChange={e => setForm(f => ({...f,[name]:e.target.value}))} />
            </div>
          ))}
          {error && <p style={{color:"var(--danger)",fontSize:"0.82rem",marginTop:"0.5rem"}}>{error}</p>}
          <div className="modal-actions">
            <button type="button" className="btn-secondary" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn-primary" disabled={loading}>{loading ? "Adding…" : "Add Item"}</button>
          </div>
        </form>
      </div>
    </div>
  );
}
