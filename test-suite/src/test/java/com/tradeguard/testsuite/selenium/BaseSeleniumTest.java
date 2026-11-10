package com.tradeguard.testsuite.selenium;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

/**
 * Base class for Selenium UI tests. Runs a headless browser (Chrome by default)
 * so the suite can execute in GitHub Actions without a display server. The driver
 * binary is resolved automatically by Selenium Manager (built into Selenium 4).
 *
 * Assumes the SUT API (default http://localhost:8080) and the React dashboard
 * (default http://localhost:5173) are already running - see the "selenium-tests"
 * job in the CI pipeline. Each test seeds its own portfolio and trades through
 * the REST API, so tests are independent of any pre-existing data.
 */
public abstract class BaseSeleniumTest {

    protected static final String DASHBOARD_URL = System.getProperty("dashboard.url", "http://localhost:5173");
    protected static final String API_URL = System.getProperty("api.url", "http://localhost:8080");
    private static final String BROWSER = System.getProperty("browser", "chrome");

    protected WebDriver driver;
    protected WebDriverWait wait;
    protected long portfolioId;

    @BeforeEach
    void setUpDriver() {
        driver = "edge".equalsIgnoreCase(BROWSER) ? edge() : chrome();
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        portfolioId = createPortfolio("10000");
    }

    @AfterEach
    void tearDownDriver() {
        if (driver != null) {
            driver.quit();
        }
    }

    private static WebDriver chrome() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--disable-gpu", "--window-size=1400,1000",
                "--no-sandbox", "--disable-dev-shm-usage");
        return new ChromeDriver(options);
    }

    private static WebDriver edge() {
        EdgeOptions options = new EdgeOptions();
        options.addArguments("--headless=new", "--disable-gpu", "--window-size=1400,1000");
        return new EdgeDriver(options);
    }

    // ---------- API seeding ----------

    protected long createPortfolio(String cash) {
        return RestAssured.given().baseUri(API_URL).contentType(ContentType.JSON)
                .body(Map.of("name", "Selenium Portfolio", "cashBalance", new BigDecimal(cash)))
                .post("/api/portfolios").then().statusCode(201).extract().jsonPath().getLong("id");
    }

    protected long createTrade(String type, String ticker, String quantity, String price) {
        return RestAssured.given().baseUri(API_URL).contentType(ContentType.JSON)
                .body(Map.of("portfolioId", portfolioId, "ticker", ticker, "tradeType", type,
                        "quantity", new BigDecimal(quantity), "price", new BigDecimal(price)))
                .post("/api/trades").then().statusCode(201).extract().jsonPath().getLong("id");
    }

    protected void apiAction(long tradeId, String action) {
        RestAssured.given().baseUri(API_URL).post("/api/trades/{id}/" + action, tradeId).then().statusCode(200);
    }

    // ---------- UI helpers ----------

    protected void openDashboard() {
        driver.get(DASHBOARD_URL);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[data-testid='trade-table']")));
    }

    protected By row(long tradeId) {
        return By.cssSelector("tr[data-trade-id='" + tradeId + "']");
    }

    protected WebElement waitForRow(long tradeId) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(row(tradeId)));
    }

    protected By rowButton(long tradeId, String testId) {
        return By.cssSelector("tr[data-trade-id='" + tradeId + "'] [data-testid='" + testId + "']");
    }

    protected void clickRowButton(long tradeId, String testId) {
        wait.until(ExpectedConditions.elementToBeClickable(rowButton(tradeId, testId))).click();
    }

    protected void waitForStatus(long tradeId, String status) {
        By badge = By.cssSelector("tr[data-trade-id='" + tradeId + "'] [data-testid='status-badge']");
        wait.until(ExpectedConditions.textToBe(badge, status));
    }

    protected String statusOf(long tradeId) {
        return driver.findElement(By.cssSelector("tr[data-trade-id='" + tradeId + "'] [data-testid='status-badge']")).getText();
    }
}
