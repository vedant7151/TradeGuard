Feature: Trade Validation
  As a trading desk operator
  I want trades to be validated against cash and position rules
  So that the firm never settles a trade it cannot afford or does not hold

  Scenario: Valid BUY trade with sufficient funds
    Given a portfolio with cash balance of 10000
    And a BUY trade for 10 units of "AAPL" at price 150.00
    When the trade is submitted for validation
    Then the trade status should be "VALIDATED"

  Scenario: BUY trade rejected due to insufficient funds
    Given a portfolio with cash balance of 500
    And a BUY trade for 10 units of "AAPL" at price 150.00
    When the trade is submitted for validation
    Then the trade status should be "REJECTED"
    And the rejection reason should contain "Insufficient funds"

  Scenario: BUY trade that costs exactly the available cash is accepted
    Given a portfolio with cash balance of 1500
    And a BUY trade for 10 units of "AAPL" at price 150.00
    When the trade is submitted for validation
    Then the trade status should be "VALIDATED"

  Scenario: Trade rejected due to zero quantity
    Given a portfolio with cash balance of 10000
    And a BUY trade for 0 units of "AAPL" at price 150.00
    When the trade is submitted for validation
    Then the trade status should be "REJECTED"
    And the rejection reason should contain "Quantity must be greater than zero"

  Scenario: Trade rejected due to negative price
    Given a portfolio with cash balance of 10000
    And a BUY trade for 10 units of "AAPL" at price -5.00
    When the trade is submitted for validation
    Then the trade status should be "REJECTED"
    And the rejection reason should contain "Price must be greater than zero"

  Scenario: SELL trade rejected when the portfolio holds no position
    Given a portfolio with cash balance of 10000
    And a SELL trade for 10 units of "AAPL" at price 150.00
    When the trade is submitted for validation
    Then the trade status should be "REJECTED"
    And the rejection reason should contain "Insufficient position"

  Scenario: SELL trade rejected when the portfolio holds too few units
    Given a portfolio with cash balance of 0 holding 5 units of "AAPL"
    And a SELL trade for 10 units of "AAPL" at price 150.00
    When the trade is submitted for validation
    Then the trade status should be "REJECTED"
    And the rejection reason should contain "Insufficient position"

  Scenario: SELL trade accepted when the portfolio holds enough units
    Given a portfolio with cash balance of 0 holding 20 units of "AAPL"
    And a SELL trade for 10 units of "AAPL" at price 150.00
    When the trade is submitted for validation
    Then the trade status should be "VALIDATED"

  Scenario: A rejected trade cannot be validated again
    Given a portfolio with cash balance of 500
    And a BUY trade for 10 units of "AAPL" at price 150.00
    And the trade has been validated
    When the trade is submitted for validation
    Then the request should be refused with status 409 and message containing "Only DRAFT"
