import { useEffect, useState, useCallback } from "react";
import TradeTable from "./components/TradeTable.jsx";
import Toast from "./components/Toast.jsx";
import { api } from "./api.js";

const ACTION_HANDLERS = {
  validate: api.validateTrade,
  match: api.matchTrade,
  settle: api.settleTrade,
  cancel: api.cancelTrade,
};

export default function App() {
  const [trades, setTrades] = useState([]);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);

  const refresh = useCallback(async () => {
    try {
      const data = await api.listTrades();
      setTrades(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  const handleAction = async (tradeId, actionKey) => {
    try {
      await ACTION_HANDLERS[actionKey](tradeId);
      await refresh();
    } catch (err) {
      setError(err.message || "Action failed");
    }
  };

  return (
    <div className="app">
      <header>
        <h1>TradeGuard Dashboard</h1>
        <p>Live view of trades in the Trade Management API</p>
      </header>

      {loading ? (
        <p>Loading trades...</p>
      ) : (
        <TradeTable trades={trades} onAction={handleAction} />
      )}

      <Toast message={error} onClose={() => setError(null)} />
    </div>
  );
}
