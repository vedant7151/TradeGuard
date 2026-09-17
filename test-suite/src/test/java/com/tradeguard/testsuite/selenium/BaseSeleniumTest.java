package com.tradeguard.testsuite.selenium;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

/**
 * Base class for Selenium UI tests. Runs headless Chrome so the suite can
 * execute in GitHub Actions without a display server. Assumes the SUT API
 * (localhost:8080) and the React dashboard (localhost:5173) are already
 * running - see the "selenium-tests" job in the CI pipeline.
 */
public abstract class BaseSeleniumTest {

    protected static final String DASHBOARD_URL = System.getProperty("dashboard.url", "http://localhost:5173");

    protected ChromeDriver driver;

    @BeforeEach
    void setUpDriver() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--disable-gpu", "--window-size=1280,900", "--no-sandbox");
        driver = new ChromeDriver(options);
    }

    @AfterEach
    void tearDownDriver() {
        if (driver != null) {
            driver.quit();
        }
    }
}
