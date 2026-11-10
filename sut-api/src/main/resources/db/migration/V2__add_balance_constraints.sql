ALTER TABLE portfolios
    ADD CONSTRAINT chk_portfolio_cash_non_negative CHECK (cash_balance >= 0);

ALTER TABLE positions
    ADD CONSTRAINT chk_position_qty_non_negative CHECK (quantity >= 0);
