import { useEffect, useState, useCallback } from "react";
import TradeTable from "./components/TradeTable.jsx";
import NewTradeForm from "./components/NewTradeForm.jsx";
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
  const [portfolios, setPortfolios] = useState([]);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);

  const refresh = useCallback(async () => {
    try {
      const [tradeData, portfolioData] = await Promise.all([api.listTrades(), api.listPortfolios()]);
      setTrades(tradeData);
      setPortfolios(portfolioData);
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
    } catch (err) {
      setError(err.message || "Action failed");
    }
    // Always re-sync so a rejected action shows the trade's real, current state.
    await refresh();
  };

  const handleCreate = async (trade) => {
    try {
      await api.createTrade(trade);
      await refresh();
      return true;
    } catch (err) {
      setError(err.message || "Could not create trade");
      return false;
    }
  };

  return (
    <div className="app">
      <header>
        <h1>TradeGuard Dashboard</h1>
        <p>Live view of trades in the Trade Management API</p>
      </header>

      <NewTradeForm portfolios={portfolios} onCreate={handleCreate} />

      <div className="toolbar">
        <h2>Trades</h2>
        <button onClick={refresh} data-testid="refresh-btn">
          Refresh
        </button>
      </div>

      {loading ? (
        <p data-testid="loading">Loading trades...</p>
      ) : (
        <TradeTable trades={trades} onAction={handleAction} />
      )}

      <Toast message={error} onClose={() => setError(null)} />
    </div>
  );
}
