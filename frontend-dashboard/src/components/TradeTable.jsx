const ACTIONS_BY_STATUS = {
  DRAFT: [
    { key: "validate", label: "Validate", testId: "validate-btn" },
    { key: "cancel", label: "Cancel", testId: "cancel-btn" },
  ],
  VALIDATED: [{ key: "match", label: "Match", testId: "match-btn" }],
  MATCHED: [{ key: "settle", label: "Settle", testId: "settle-btn" }],
};

export default function TradeTable({ trades, onAction }) {
  return (
    <table data-testid="trade-table">
      <thead>
        <tr>
          <th>Trade Ref</th>
          <th>Ticker</th>
          <th>Type</th>
          <th>Qty</th>
          <th>Price</th>
          <th>Status</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        {trades.map((trade) => (
          <tr key={trade.id} data-trade-id={trade.id}>
            <td>{trade.tradeRef}</td>
            <td>{trade.ticker}</td>
            <td>{trade.tradeType}</td>
            <td>{trade.quantity}</td>
            <td>{trade.price}</td>
            <td>
              <span
                className={`status-badge status-${trade.status.toLowerCase()}`}
                data-testid="status-badge"
              >
                {trade.status}
              </span>
            </td>
            <td>
              {(ACTIONS_BY_STATUS[trade.status] || []).map((action) => (
                <button
                  key={action.key}
                  data-testid={action.testId}
                  onClick={() => onAction(trade.id, action.key)}
                >
                  {action.label}
                </button>
              ))}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
