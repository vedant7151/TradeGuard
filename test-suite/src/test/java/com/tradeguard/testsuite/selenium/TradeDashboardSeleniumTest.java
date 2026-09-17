package com.tradeguard.testsuite.selenium;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TradeDashboardSeleniumTest extends BaseSeleniumTest {

    private WebDriverWait wait;

    @Test
    void dashboardLoads_andTradeTableIsVisible() {
        driver.get(DASHBOARD_URL);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        WebElement table = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[data-testid='trade-table']")));

        assertThat(table.isDisplayed()).isTrue();
    }

    @Test
    void clickingValidateOnDraftTrade_updatesStatusBadge() {
        driver.get(DASHBOARD_URL);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[data-testid='trade-table']")));

        List<WebElement> validateButtons = driver.findElements(By.cssSelector("[data-testid='validate-btn']"));
        assertThat(validateButtons).isNotEmpty();

        WebElement firstButton = validateButtons.get(0);
        String row = firstButton.findElement(By.xpath("ancestor::tr")).getAttribute("data-trade-id");
        firstButton.click();

        WebElement statusBadge = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("tr[data-trade-id='" + row + "'] [data-testid='status-badge']")));

        wait.until(d -> !statusBadge.getText().equalsIgnoreCase("DRAFT"));

        assertThat(statusBadge.getText()).isIn("VALIDATED", "REJECTED");
    }

    @Test
    void invalidAction_showsErrorToast() {
        driver.get(DASHBOARD_URL);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[data-testid='trade-table']")));

        List<WebElement> settleButtons = driver.findElements(By.cssSelector("[data-testid='settle-btn']"));
        if (!settleButtons.isEmpty()) {
            settleButtons.get(0).click();
            WebElement toast = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[data-testid='error-toast']")));
            assertThat(toast.isDisplayed()).isTrue();
        }
    }
}
