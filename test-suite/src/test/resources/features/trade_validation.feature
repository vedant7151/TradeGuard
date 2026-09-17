Feature: Trade Validation
  As a trading desk operator
  I want trades to be validated against cash and position rules
  So that the firm never settles a trade it cannot afford or does not hold

  Background:
    Given a portfolio with cash balance of 10000

  Scenario: Valid BUY trade with sufficient funds
    Given a BUY trade for 10 units of "AAPL" at price 150.00
    When the trade is submitted for validation
    Then the trade status should be "VALIDATED"

  Scenario: BUY trade rejected due to insufficient funds
    Given a BUY trade for 1000 units of "AAPL" at price 150.00
    When the trade is submitted for validation
    Then the trade status should be "REJECTED"
    And the rejection reason should contain "Insufficient funds"

  Scenario: BUY trade rejected due to zero quantity
    Given a BUY trade for 0 units of "AAPL" at price 150.00
    When the trade is submitted for validation
    Then the trade status should be "REJECTED"
    And the rejection reason should contain "Quantity must be greater than zero"

  Scenario: BUY trade rejected due to negative price
    Given a BUY trade for 10 units of "AAPL" at price -5.00
    When the trade is submitted for validation
    Then the trade status should be "REJECTED"
    And the rejection reason should contain "Price must be greater than zero"

  Scenario: SELL trade rejected when portfolio holds no position
    Given a SELL trade for 10 units of "AAPL" at price 150.00
    When the trade is submitted for validation
    Then the trade status should be "REJECTED"
    And the rejection reason should contain "Insufficient position"
