const BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";

async function request(path, options = {}) {
  const response = await fetch(`${BASE_URL}${path}`, {
    headers: { "Content-Type": "application/json" },
    ...options,
  });

  if (!response.ok) {
    const body = await response.json().catch(() => ({ message: response.statusText }));
    const error = new Error(body.message || "Request failed");
    error.status = response.status;
    throw error;
  }

  if (response.status === 204) return null;
  return response.json();
}

export const api = {
  listTrades: () => request("/trades"),
  validateTrade: (id) => request(`/trades/${id}/validate`, { method: "POST" }),
  matchTrade: (id) => request(`/trades/${id}/match`, { method: "POST" }),
  settleTrade: (id) => request(`/trades/${id}/settle`, { method: "POST" }),
  cancelTrade: (id) => request(`/trades/${id}`, { method: "DELETE" }),
};
