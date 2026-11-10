Feature: Trade Lifecycle
  As a trading desk operator
  I want a validated trade to move through matching and settlement
  So that positions and cash balances reflect completed trades

  Scenario: A validated BUY trade can be matched and settled
    Given a portfolio with cash balance of 10000
    And a BUY trade for 10 units of "AAPL" at price 150.00
    And the trade has been validated
    When the trade is matched
    And the trade is settled
    Then the trade status should be "SETTLED"
    And the trade should have a settlement timestamp
    And the portfolio cash balance should be 8500.00
    And the portfolio should hold 10 units of "AAPL"

  Scenario: Settling a SELL trade credits cash and reduces the position
    Given a portfolio with cash balance of 1000 holding 20 units of "AAPL"
    And a SELL trade for 8 units of "AAPL" at price 150.00
    And the trade has been validated
    And the trade has been matched
    When the trade is settled
    Then the trade status should be "SETTLED"
    And the portfolio cash balance should be 2200.00
    And the portfolio should hold 12 units of "AAPL"

  Scenario: Selling an entire position leaves no units held
    Given a portfolio with cash balance of 0 holding 10 units of "MSFT"
    And a SELL trade for 10 units of "MSFT" at price 100.00
    And the trade has been validated
    And the trade has been matched
    When the trade is settled
    Then the portfolio cash balance should be 1000.00
    And the portfolio should hold no units of "MSFT"

  Scenario: A DRAFT trade cannot be settled directly
    Given a portfolio with cash balance of 10000
    And a BUY trade for 10 units of "AAPL" at price 150.00
    When the trade is settled
    Then the request should be refused with status 409 and message containing "Only MATCHED trades"
    And the trade status should be "DRAFT"
    And the portfolio cash balance should be 10000

  Scenario: A VALIDATED trade cannot be settled without matching
    Given a portfolio with cash balance of 10000
    And a BUY trade for 10 units of "AAPL" at price 150.00
    And the trade has been validated
    When the trade is settled
    Then the request should be refused with status 409 and message containing "Only MATCHED trades"

  Scenario: A rejected trade cannot be matched
    Given a portfolio with cash balance of 100
    And a BUY trade for 10 units of "AAPL" at price 150.00
    And the trade has been validated
    When the trade is matched
    Then the request should be refused with status 409 and message containing "Only VALIDATED trades"

  Scenario: A settled trade cannot be settled twice
    Given a portfolio with cash balance of 10000
    And a BUY trade for 10 units of "AAPL" at price 150.00
    And the trade has been validated
    And the trade has been matched
    And the trade has been settled
    When the trade is settled
    Then the request should be refused with status 409 and message containing "Only MATCHED trades"
    And the portfolio cash balance should be 8500.00
    And the portfolio should hold 10 units of "AAPL"

  Scenario: A DRAFT trade can be cancelled
    Given a portfolio with cash balance of 10000
    And a BUY trade for 10 units of "AAPL" at price 150.00
    When the trade is cancelled
    Then the trade status should be "CANCELLED"

  Scenario: A MATCHED trade cannot be cancelled
    Given a portfolio with cash balance of 10000
    And a BUY trade for 10 units of "AAPL" at price 150.00
    And the trade has been validated
    And the trade has been matched
    When the trade is cancelled
    Then the request should be refused with status 409 and message containing "Only DRAFT trades"
    And the trade status should be "MATCHED"

  Scenario: A SETTLED trade cannot be cancelled
    Given a portfolio with cash balance of 10000
    And a BUY trade for 10 units of "AAPL" at price 150.00
    And the trade has been validated
    And the trade has been matched
    And the trade has been settled
    When the trade is cancelled
    Then the request should be refused with status 409 and message containing "Only DRAFT trades"
    And the trade status should be "SETTLED"
