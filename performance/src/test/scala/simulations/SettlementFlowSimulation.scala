package simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._

/**
 * Settle-path load: the full lifecycle (create -> validate -> match -> settle) for many users.
 *
 * Every virtual user creates its OWN portfolio, so users never contend on one account row and
 * the final balance is deterministic. After its settlements each user checks that the money
 * and the position are exactly right - a correctness check under load, not only a latency one:
 *   cash     = opening cash - settlements x notional
 *   position = settlements x quantity
 *
 * Asserts: overall p95 < 500 ms, settle-step p95 < 500 ms, error rate < 1 %.
 *
 * Run with: mvn gatling:test -Dgatling.simulationClass=simulations.SettlementFlowSimulation
 */
class SettlementFlowSimulation extends Simulation {

  val baseUrl: String = System.getProperty("gatling.baseUrl", "http://localhost:8080")
  val users: Int = Integer.getInteger("gatling.users", 25)
  val settlementsPerUser: Int = Integer.getInteger("gatling.settlementsPerUser", 5)
  val rampSeconds: Int = Integer.getInteger("gatling.rampSeconds", 10)

  private val openingCash = 1000000
  private val unitPrice = 10
  private val expectedCash = (openingCash - settlementsPerUser * unitPrice).toDouble
  private val expectedQuantity = settlementsPerUser.toDouble

  val httpProtocol = http
    .baseUrl(baseUrl)
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")

  val settleScenario = scenario("Create, validate, match and settle")
    .exec(
      http("Create Portfolio")
        .post("/api/portfolios")
        .body(StringBody(s"""{"name":"Gatling Settlement Portfolio","cashBalance":$openingCash}"""))
        .check(status.is(201))
        .check(jsonPath("$.id").saveAs("portfolioId"))
    )
    .repeat(settlementsPerUser) {
      exec(
        http("Create Trade")
          .post("/api/trades")
          .body(StringBody(
            s"""{"portfolioId": #{portfolioId}, "ticker": "AAPL", "tradeType": "BUY", "quantity": 1, "price": $unitPrice}"""))
          .check(status.is(201))
          .check(jsonPath("$.id").saveAs("tradeId"))
      ).exec(
        http("Validate Trade")
          .post("/api/trades/#{tradeId}/validate")
          .check(status.is(200))
          .check(jsonPath("$.status").is("VALIDATED"))
      ).exec(
        http("Match Trade")
          .post("/api/trades/#{tradeId}/match")
          .check(status.is(200))
          .check(jsonPath("$.status").is("MATCHED"))
      ).exec(
        http("Settle Trade")
          .post("/api/trades/#{tradeId}/settle")
          .check(status.is(200))
          .check(jsonPath("$.status").is("SETTLED"))
      ).pause(500.milliseconds, 1.second)
    }
    .exec(
      http("Check Portfolio Cash")
        .get("/api/portfolios/#{portfolioId}")
        .check(status.is(200))
        .check(jsonPath("$.cashBalance").ofType[Double].is(expectedCash))
    )
    .exec(
      http("Check Position")
        .get("/api/portfolios/#{portfolioId}/positions")
        .check(status.is(200))
        .check(jsonPath("$[0].quantity").ofType[Double].is(expectedQuantity))
    )

  setUp(
    settleScenario.inject(rampUsers(users).during(rampSeconds.seconds))
  ).protocols(httpProtocol)
    .assertions(
      global.responseTime.percentile(95).lt(500),
      details("Settle Trade").responseTime.percentile(95).lt(500),
      global.failedRequests.percent.lt(1.0)
    )
}
