const BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";

async function request(path, options = {}) {
  let response;
  try {
    response = await fetch(`${BASE_URL}${path}`, {
      headers: { "Content-Type": "application/json" },
      ...options,
    });
  } catch {
    throw new Error("Cannot reach the Trade API");
  }

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
  listPortfolios: () => request("/portfolios"),
  createTrade: (trade) => request("/trades", { method: "POST", body: JSON.stringify(trade) }),
  validateTrade: (id) => request(`/trades/${id}/validate`, { method: "POST" }),
  matchTrade: (id) => request(`/trades/${id}/match`, { method: "POST" }),
  settleTrade: (id) => request(`/trades/${id}/settle`, { method: "POST" }),
  cancelTrade: (id) => request(`/trades/${id}`, { method: "DELETE" }),
};
