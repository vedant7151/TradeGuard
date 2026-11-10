package com.tradeguard.testsuite.bdd;

import com.tradeguard.sutapi.repository.PortfolioRepository;
import com.tradeguard.sutapi.repository.PositionRepository;
import com.tradeguard.sutapi.entity.Trade;
import com.tradeguard.sutapi.enums.TradeStatus;
import com.tradeguard.sutapi.enums.TradeType;
import com.tradeguard.sutapi.repository.TradeRepository;
import java.time.Instant;
import java.util.UUID;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions. Everything goes through the public REST API with REST Assured;
 * no repositories are touched, so the scenarios behave like a real client.
 */
public class TradeSteps {

    @Autowired
    private TradeRepository tradeRepository;

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Value("${local.server.port}")
    private int port;

    private Long portfolioId;
    private Long tradeId;
    private Response lastResponse;

    @Before
    public void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        portfolioId = null;
        tradeId = null;
        lastResponse = null;
    }

    @After
    public void tearDown() {
        tradeRepository.deleteAll();
        positionRepository.deleteAll();
        portfolioRepository.deleteAll();
    }

    // ---------- Given ----------

    @Given("a portfolio with cash balance of {bigdecimal}")
    public void aPortfolioWithCashBalanceOf(BigDecimal cash) {
        createPortfolio(cash, List.of());
    }

    @Given("a portfolio with cash balance of {bigdecimal} holding {bigdecimal} units of {string}")
    public void aPortfolioHolding(BigDecimal cash, BigDecimal quantity, String ticker) {
        createPortfolio(cash, List.of(Map.of("ticker", ticker, "quantity", quantity)));
    }

    @Given("a {word} trade for {bigdecimal} units of {string} at price {bigdecimal}")
    public void aTradeForUnitsAtPrice(String type, BigDecimal quantity, String ticker, BigDecimal price) {
        if (quantity.compareTo(BigDecimal.ZERO) <= 0 || price.compareTo(BigDecimal.ZERO) <= 0) {
            Trade trade = tradeRepository.save(Trade.builder()
                    .portfolioId(portfolioId)
                    .ticker(ticker)
                    .tradeType(TradeType.valueOf(type))
                    .quantity(quantity)
                    .price(price)
                    .status(TradeStatus.DRAFT)
                    .tradeRef("TRD-" + UUID.randomUUID())
                    .createdAt(Instant.now())
                    .build());
            tradeId = trade.getId();
            return;
        }
        lastResponse = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("portfolioId", portfolioId, "ticker", ticker, "tradeType", type,
                        "quantity", quantity, "price", price))
                .post("/api/trades");
        assertThat(lastResponse.statusCode()).as(lastResponse.asString()).isEqualTo(201);
        tradeId = lastResponse.jsonPath().getLong("id");
    }

    @Given("the trade has been validated")
    public void theTradeHasBeenValidated() {
        post("validate");
        assertThat(lastResponse.statusCode()).isEqualTo(200);
    }

    @Given("the trade has been matched")
    public void theTradeHasBeenMatched() {
        post("match");
        assertThat(lastResponse.statusCode()).isEqualTo(200);
    }

    @Given("the trade has been settled")
    public void theTradeHasBeenSettled() {
        post("settle");
        assertThat(lastResponse.statusCode()).isEqualTo(200);
    }

    // ---------- When ----------

    @When("the trade is submitted for validation")
    public void theTradeIsSubmittedForValidation() {
        post("validate");
    }

    @When("the trade is matched")
    public void theTradeIsMatched() {
        post("match");
    }

    @When("the trade is settled")
    public void theTradeIsSettled() {
        post("settle");
    }

    @When("the trade is cancelled")
    public void theTradeIsCancelled() {
        lastResponse = RestAssured.given().delete("/api/trades/{id}", tradeId);
    }

    @When("a trade is requested for a portfolio that does not exist")
    public void aTradeIsRequestedForUnknownPortfolio() {
        lastResponse = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("portfolioId", 999999, "ticker", "AAPL", "tradeType", "BUY",
                        "quantity", 1, "price", 1))
                .post("/api/trades");
    }

    @When("a trade is requested without a ticker")
    public void aTradeIsRequestedWithoutTicker() {
        lastResponse = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("portfolioId", portfolioId, "tradeType", "BUY", "quantity", 1, "price", 1))
                .post("/api/trades");
    }

    @When("the trade with id {long} is requested")
    public void theTradeWithIdIsRequested(long id) {
        lastResponse = RestAssured.given().get("/api/trades/{id}", id);
    }

    @When("trades are listed with status {string}")
    public void tradesAreListedWithStatus(String status) {
        lastResponse = RestAssured.given().queryParam("status", status).get("/api/trades");
    }

    // ---------- Then ----------

    @Then("the trade status should be {string}")
    public void theTradeStatusShouldBe(String status) {
        Response current = RestAssured.given().get("/api/trades/{id}", tradeId);
        assertThat(current.statusCode()).isEqualTo(200);
        assertThat(current.jsonPath().getString("status")).isEqualTo(status);
    }

    @Then("the rejection reason should contain {string}")
    public void theRejectionReasonShouldContain(String fragment) {
        Response current = RestAssured.given().get("/api/trades/{id}", tradeId);
        assertThat(current.jsonPath().getString("rejectionReason")).containsIgnoringCase(fragment);
    }

    @Then("the request should be refused with status {int} and message containing {string}")
    public void theRequestShouldBeRefused(int status, String fragment) {
        assertThat(lastResponse.statusCode()).as(lastResponse.asString()).isEqualTo(status);
        assertThat(lastResponse.jsonPath().getString("message")).containsIgnoringCase(fragment);
    }

    @Then("the trade should have a settlement timestamp")
    public void theTradeShouldHaveASettlementTimestamp() {
        Response current = RestAssured.given().get("/api/trades/{id}", tradeId);
        assertThat(current.jsonPath().getString("settledAt")).isNotBlank();
    }

    @Then("the portfolio cash balance should be {bigdecimal}")
    public void thePortfolioCashBalanceShouldBe(BigDecimal expected) {
        Response portfolio = RestAssured.given().get("/api/portfolios/{id}", portfolioId);
        assertThat(new BigDecimal(portfolio.jsonPath().getString("cashBalance"))).isEqualByComparingTo(expected);
    }

    @Then("the portfolio should hold {bigdecimal} units of {string}")
    public void thePortfolioShouldHoldUnitsOf(BigDecimal expected, String ticker) {
        assertThat(holdingOf(ticker)).isEqualByComparingTo(expected);
    }

    @Then("the portfolio should hold no units of {string}")
    public void thePortfolioShouldHoldNoUnitsOf(String ticker) {
        assertThat(holdingOf(ticker)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Then("every returned trade should have status {string}")
    public void everyReturnedTradeShouldHaveStatus(String status) {
        assertThat(lastResponse.statusCode()).isEqualTo(200);
        List<String> statuses = lastResponse.jsonPath().getList("status", String.class);
        assertThat(statuses).isNotEmpty().allMatch(status::equals);
    }

    @Then("the returned trades should include the trade under test")
    public void theReturnedTradesShouldIncludeTheTradeUnderTest() {
        List<Integer> ids = lastResponse.jsonPath().getList("id", Integer.class);
        assertThat(ids).contains(tradeId.intValue());
    }

    // ---------- helpers ----------

    private void createPortfolio(BigDecimal cash, List<Map<String, Object>> positions) {
        Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", "BDD Portfolio", "cashBalance", cash, "positions", positions))
                .post("/api/portfolios");
        assertThat(response.statusCode()).as(response.asString()).isEqualTo(201);
        portfolioId = response.jsonPath().getLong("id");
    }

    private void post(String action) {
        lastResponse = RestAssured.given().post("/api/trades/{id}/" + action, tradeId);
    }

    private BigDecimal holdingOf(String ticker) {
        Response positions = RestAssured.given().get("/api/portfolios/{id}/positions", portfolioId);
        assertThat(positions.statusCode()).isEqualTo(200);
        List<Map<String, Object>> all = positions.jsonPath().getList("$");
        return all.stream()
                .filter(p -> ticker.equals(p.get("ticker")))
                .map(p -> new BigDecimal(p.get("quantity").toString()))
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }
}
