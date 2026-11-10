import { useState } from "react";

const EMPTY = { ticker: "", tradeType: "BUY", quantity: "", price: "" };

export default function NewTradeForm({ portfolios, onCreate }) {
  const [form, setForm] = useState(EMPTY);
  const [portfolioId, setPortfolioId] = useState("");

  const selectedPortfolio = portfolioId || (portfolios[0] ? String(portfolios[0].id) : "");
  const update = (field) => (e) => setForm({ ...form, [field]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    const created = await onCreate({
      portfolioId: Number(selectedPortfolio),
      ticker: form.ticker.trim().toUpperCase(),
      tradeType: form.tradeType,
      quantity: form.quantity,
      price: form.price,
    });
    if (created) setForm(EMPTY);
  };

  return (
    <form className="new-trade" onSubmit={submit} data-testid="new-trade-form">
      <h2>New trade</h2>
      <div className="fields">
        <label>
          Portfolio
          <select
            data-testid="portfolio-select"
            value={selectedPortfolio}
            onChange={(e) => setPortfolioId(e.target.value)}
          >
            {portfolios.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name} (cash {p.cashBalance})
              </option>
            ))}
          </select>
        </label>
        <label>
          Ticker
          <input data-testid="ticker-input" value={form.ticker} onChange={update("ticker")} required />
        </label>
        <label>
          Type
          <select data-testid="type-select" value={form.tradeType} onChange={update("tradeType")}>
            <option value="BUY">BUY</option>
            <option value="SELL">SELL</option>
          </select>
        </label>
        <label>
          Quantity
          <input
            data-testid="quantity-input"
            type="number"
            step="any"
            value={form.quantity}
            onChange={update("quantity")}
            required
          />
        </label>
        <label>
          Price
          <input
            data-testid="price-input"
            type="number"
            step="any"
            value={form.price}
            onChange={update("price")}
            required
          />
        </label>
        <button type="submit" data-testid="create-trade-btn" disabled={!selectedPortfolio}>
          Create
        </button>
      </div>
    </form>
  );
}
