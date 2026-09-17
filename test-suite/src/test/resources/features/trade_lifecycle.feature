Feature: Trade Lifecycle
  As a trading desk operator
  I want a validated trade to move through matching and settlement
  So that positions and cash balances reflect completed trades

  Background:
    Given a portfolio with cash balance of 10000

  Scenario: A validated trade can be matched and settled
    Given a BUY trade for 10 units of "AAPL" at price 150.00
    And the trade is submitted for validation
    When the trade is matched
    And the trade is settled
    Then the trade status should be "SETTLED"
    And the portfolio cash balance should be 8500.00
    And the portfolio should hold 10 units of "AAPL"

  Scenario: A DRAFT trade cannot be settled directly
    Given a BUY trade for 10 units of "AAPL" at price 150.00
    When the trade is settled
    Then the settlement should fail with status "MATCHED trades"

  Scenario: A DRAFT trade can be cancelled
    Given a BUY trade for 10 units of "AAPL" at price 150.00
    When the trade is cancelled
    Then the trade status should be "CANCELLED"
