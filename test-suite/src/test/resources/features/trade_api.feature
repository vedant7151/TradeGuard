Feature: Trade API behaviour
  As an integrating client application
  I want clear, predictable API responses
  So that I can surface errors and query trades reliably

  Scenario: Requesting an unknown trade returns not found
    When the trade with id 999999 is requested
    Then the request should be refused with status 404 and message containing "Trade not found"

  Scenario: Creating a trade for an unknown portfolio returns not found
    When a trade is requested for a portfolio that does not exist
    Then the request should be refused with status 404 and message containing "Portfolio not found"

  Scenario: Creating a trade without a ticker is a bad request
    Given a portfolio with cash balance of 10000
    When a trade is requested without a ticker
    Then the request should be refused with status 400 and message containing "ticker"

  Scenario: Trades can be listed by status
    Given a portfolio with cash balance of 10000
    And a BUY trade for 1 units of "AAPL" at price 10.00
    And the trade has been validated
    When trades are listed with status "VALIDATED"
    Then every returned trade should have status "VALIDATED"
    And the returned trades should include the trade under test

  Scenario: Listing trades with an unknown status is a bad request
    When trades are listed with status "BOGUS"
    Then the request should be refused with status 400 and message containing "status"
