package simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import scala.util.Random

/**
 * Simulates 50 concurrent users each submitting 10 trades (create -> validate)
 * against the TradeGuard SUT API. Asserts the non-functional SLA from the PRD:
 * p95 response time under 500ms and error rate under 1%.
 *
 * Run with: mvn gatling:test -Dgatling.baseUrl=http://localhost:8080
 */
class TradeSubmissionSimulation extends Simulation {

  val baseUrl: String = System.getProperty("gatling.baseUrl", "http://localhost:8080")

  val httpProtocol = http
    .baseUrl(baseUrl)
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")

  val tickers = Array("AAPL", "MSFT", "GOOG", "AMZN", "TSLA")

  val submitTradeScenario = scenario("Submit and validate trades")
    .repeat(10) {
      exec(
        http("Create Trade")
          .post("/api/trades")
          .body(StringBody(session => {
            val ticker = tickers(Random.nextInt(tickers.length))
            s"""{
                  "portfolioId": 1,
                  "ticker": "$ticker",
                  "tradeType": "BUY",
                  "quantity": 1,
                  "price": 10.00
                }"""
          }))
          .check(status.is(201))
          .check(jsonPath("$.id").saveAs("tradeId"))
      ).exec(
        http("Validate Trade")
          .post("/api/trades/${tradeId}/validate")
          .check(status.is(200))
      )
    }

  setUp(
    submitTradeScenario.inject(rampUsers(50).during(30.seconds))
  ).protocols(httpProtocol)
    .assertions(
      global.responseTime.percentile(95).lt(500),
      global.failedRequests.percent.lt(1.0)
    )
}
