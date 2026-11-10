package simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import scala.concurrent.duration._
import scala.util.Random

/**
 * PRD 7.5: 50 concurrent users each submitting 10 trades.
 *
 * Each iteration creates a trade, validates it and reads it back, then "thinks" for 1.5-2.5 s.
 * Users are ramped in over 10 s, and every user's session lasts at least 10 x 1.5 s = 15 s, so
 * the first user is still running when the last one arrives: all 50 sessions overlap.
 * Confirm the peak in the Gatling report's "Active users along the simulation" chart.
 *
 * Asserts the non-functional SLAs:
 *   - p95 response time < 500 ms
 *   - error rate < 1 %
 * The HTML report additionally shows throughput and p50 / p95 / p99 percentiles.
 *
 * Run with: mvn gatling:test -Dgatling.baseUrl=http://localhost:8080
 */
class TradeSubmissionSimulation extends Simulation {

  val baseUrl: String = System.getProperty("gatling.baseUrl", "http://localhost:8080")
  val users: Int = Integer.getInteger("gatling.users", 50)
  val tradesPerUser: Int = Integer.getInteger("gatling.tradesPerUser", 10)
  val rampSeconds: Int = Integer.getInteger("gatling.rampSeconds", 10)

  // Populated in before(): a dedicated, well-funded portfolio so the load test never
  // depends on pre-existing data and no BUY is ever rejected for lack of cash.
  @volatile private var portfolioId: Long = 0L

  before {
    val body = """{"name":"Gatling Portfolio","cashBalance":1000000000}"""
    val request = HttpRequest.newBuilder(URI.create(s"$baseUrl/api/portfolios"))
      .header("Content-Type", "application/json")
      .POST(HttpRequest.BodyPublishers.ofString(body))
      .build()
    val response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString())
    require(response.statusCode() == 201, s"Could not create load-test portfolio: ${response.statusCode()} ${response.body()}")
    portfolioId = """"id":(\d+)""".r.findFirstMatchIn(response.body()).get.group(1).toLong
  }

  val httpProtocol = http
    .baseUrl(baseUrl)
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")

  val tickers = Array("AAPL", "MSFT", "GOOG", "AMZN", "TSLA")

  val submitTradeScenario = scenario("Submit and validate trades")
    .repeat(tradesPerUser) {
      exec(
        http("Create Trade")
          .post("/api/trades")
          .body(StringBody(_ => {
            val ticker = tickers(Random.nextInt(tickers.length))
            s"""{
                  "portfolioId": $portfolioId,
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
          .post("/api/trades/#{tradeId}/validate")
          .check(status.is(200))
          .check(jsonPath("$.status").is("VALIDATED"))
      ).exec(
        http("Get Trade")
          .get("/api/trades/#{tradeId}")
          .check(status.is(200))
      ).pause(1500.milliseconds, 2500.milliseconds)
    }

  setUp(
    submitTradeScenario.inject(rampUsers(users).during(rampSeconds.seconds))
  ).protocols(httpProtocol)
    .assertions(
      global.responseTime.percentile(95).lt(500),
      global.failedRequests.percent.lt(1.0)
    )
}
