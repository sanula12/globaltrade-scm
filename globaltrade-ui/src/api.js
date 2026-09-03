// api.js — all backend API calls in one place.
// Using a relative URL (/globaltrade/...) so Vite's dev proxy
// forwards requests to Payara on port 8080 server-side.
// This avoids ALL cross-origin/CORS/ERR_INTERNET_DISCONNECTED issues.

const BASE = "/globaltrade";

function authHeader(username, password) {
  return "Basic " + btoa(username + ":" + password);
}

async function req(method, path, credentials, params, body) {
  const { username, password } = credentials;

  let url = BASE + path;
  if (params) {
    url += "?" + new URLSearchParams(params).toString();
  }

  const opts = {
    method: method,
    headers: {
      Authorization: authHeader(username, password),
    },
  };

  if (body) {
    opts.headers["Content-Type"] = "application/x-www-form-urlencoded";
    opts.body = new URLSearchParams(body).toString();
  }

  const res = await fetch(url, opts);
  let data = {};
  try { data = await res.json(); } catch (_) { /* empty body */ }

  if (!res.ok) {
    const err = new Error(data.error || ("HTTP " + res.status));
    err.status = res.status;
    throw err;
  }
  return data;
}

// Who am I
export function getMe(creds) {
  return req("GET", "/me", creds);
}

// Shipments
export function getAllShipments(creds) {
  return req("GET", "/shipments", creds);
}
export function getShipmentsByStatus(creds, status) {
  return req("GET", "/shipments", creds, { status: status });
}
export function getShipmentById(creds, id) {
  return req("GET", "/shipments", creds, { id: id });
}
export function getShipmentByTracking(creds, tracking) {
  return req("GET", "/shipments", creds, { tracking: tracking });
}
export function createShipment(creds, body) {
  return req("POST", "/shipments", creds, null, body);
}
export function updateShipmentStatus(creds, id, status) {
  return req("PUT", "/shipments", creds, { id: id, status: status });
}
export function clearCustoms(creds, id) {
  return req("PUT", "/shipments", creds, { id: id, action: "customs" });
}
export function cancelShipment(creds, id) {
  return req("DELETE", "/shipments", creds, { id: id });
}

// Inventory
export function getAllInventory(creds) {
  return req("GET", "/inventory", creds);
}
export function getLowStock(creds) {
  return req("GET", "/inventory", creds, { low: "true" });
}
export function addInventory(creds, body) {
  return req("POST", "/inventory", creds, null, body);
}
export function restockInventory(creds, sku, qty) {
  return req("PUT", "/inventory", creds, { sku: sku, qty: qty });
}
