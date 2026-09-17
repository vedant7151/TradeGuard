package com.tradeguard.testsuite.bdd;

import com.tradeguard.sutapi.entity.Portfolio;
import com.tradeguard.sutapi.repository.PortfolioRepository;
import com.tradeguard.sutapi.repository.PositionRepository;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

public class TradeSteps {

    @Autowired
    private CucumberSpringConfiguration serverConfig;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private PositionRepository positionRepository;

    private Long portfolioId;
    private Long tradeId;
    private Response lastResponse;
    private String pendingTicker;
    private String pendingType;
    private BigDecimal pendingQuantity;
    private BigDecimal pendingPrice;

    @Before
    public void setUp() {
        RestAssured.baseURI = "http://localhost:" + serverConfig.getPort();
    }

    @Given("a portfolio with cash balance of {double}")
    public void a_portfolio_with_cash_balance_of(double cash) {
        Portfolio portfolio = portfolioRepository.save(
                Portfolio.builder().name("BDD Portfolio").cashBalance(BigDecimal.valueOf(cash)).build());
        portfolioId = portfolio.getId();
    }

    @Given("a {word} trade for {double} units of {string} at price {double}")
    public void a_trade_for_units_at_price(String type, double quantity, String ticker, double price) {
        String body = """
                {
                  "portfolioId": %d,
                  "ticker": "%s",
                  "tradeType": "%s",
                  "quantity": %s,
                  "price": %s
                }
                """.formatted(portfolioId, ticker, type, quantity, price);

        lastResponse = RestAssured.given()
                .contentType("application/json")
                .body(body)
                .post("/api/trades");

        tradeId = lastResponse.jsonPath().getLong("id");
    }

    @Given("the trade is submitted for validation")
    @When("the trade is submitted for validation")
    public void the_trade_is_submitted_for_validation() {
        lastResponse = RestAssured.given().post("/api/trades/{id}/validate", tradeId);
    }

    @When("the trade is matched")
    public void the_trade_is_matched() {
        lastResponse = RestAssured.given().post("/api/trades/{id}/match", tradeId);
    }

    @When("the trade is settled")
    public void the_trade_is_settled() {
        lastResponse = RestAssured.given().post("/api/trades/{id}/settle", tradeId);
    }

    @When("the trade is cancelled")
    public void the_trade_is_cancelled() {
        lastResponse = RestAssured.given().delete("/api/trades/{id}", tradeId);
    }

    @Then("the trade status should be {string}")
    public void the_trade_status_should_be(String status) {
        Response getResponse = RestAssured.given().get("/api/trades/{id}", tradeId);
        assertThat(getResponse.jsonPath().getString("status")).isEqualTo(status);
    }

    @Then("the rejection reason should contain {string}")
    public void the_rejection_reason_should_contain(String fragment) {
        Response getResponse = RestAssured.given().get("/api/trades/{id}", tradeId);
        assertThat(getResponse.jsonPath().getString("rejectionReason")).containsIgnoringCase(fragment);
    }

    @Then("the settlement should fail with status {string}")
    public void the_settlement_should_fail_with_status(String fragment) {
        assertThat(lastResponse.statusCode()).isEqualTo(409);
        assertThat(lastResponse.jsonPath().getString("message")).containsIgnoringCase(fragment);
    }

    @Then("the portfolio cash balance should be {double}")
    public void the_portfolio_cash_balance_should_be(double expected) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId).orElseThrow();
        assertThat(portfolio.getCashBalance()).isEqualByComparingTo(BigDecimal.valueOf(expected));
    }

    @Then("the portfolio should hold {double} units of {string}")
    public void the_portfolio_should_hold_units_of(double expected, String ticker) {
        assertThat(positionRepository.findByPortfolioIdAndTicker(portfolioId, ticker))
                .isPresent()
                .get()
                .satisfies(position -> assertThat(position.getQuantity()).isEqualByComparingTo(BigDecimal.valueOf(expected)));
    }
}
