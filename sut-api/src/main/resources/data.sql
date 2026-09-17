INSERT INTO portfolios (id, name, cash_balance)
SELECT 1, 'Demo Portfolio', 100000.0000
WHERE NOT EXISTS (SELECT 1 FROM portfolios WHERE id = 1);
