package com.tradeguard.testsuite.selenium;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

import static org.assertj.core.api.Assertions.assertThat;

class TradeDashboardSeleniumTest extends BaseSeleniumTest {

    @Test
    void dashboardLoads_andTradeTableIsVisible() {
        openDashboard();

        assertThat(driver.getTitle()).contains("TradeGuard");
        assertThat(driver.findElement(By.cssSelector("[data-testid='trade-table']")).isDisplayed()).isTrue();
        assertThat(driver.findElements(By.cssSelector("[data-testid='new-trade-form']"))).hasSize(1);
    }

    @Test
    void existingDraftTrade_isListedWithItsDetails() {
        long id = createTrade("BUY", "AAPL", "10", "150.00");

        openDashboard();

        WebElement row = waitForRow(id);
        assertThat(row.getText()).contains("AAPL").contains("BUY").contains("DRAFT");
        assertThat(driver.findElements(rowButton(id, "validate-btn"))).hasSize(1);
        assertThat(driver.findElements(rowButton(id, "cancel-btn"))).hasSize(1);
    }

    @Test
    void clickingValidate_onAffordableDraftTrade_movesToValidated() {
        long id = createTrade("BUY", "AAPL", "10", "150.00");
        openDashboard();
        waitForRow(id);

        clickRowButton(id, "validate-btn");

        waitForStatus(id, "VALIDATED");
        assertThat(driver.findElements(rowButton(id, "match-btn"))).hasSize(1);
        assertThat(driver.findElements(rowButton(id, "validate-btn"))).isEmpty();
    }

    @Test
    void clickingValidate_onUnaffordableTrade_showsRejectedWithReason() {
        long id = createTrade("BUY", "AAPL", "1000", "150.00");
        openDashboard();
        waitForRow(id);

        clickRowButton(id, "validate-btn");

        waitForStatus(id, "REJECTED");
        WebElement reason = driver.findElement(By.cssSelector("tr[data-trade-id='" + id + "'] [data-testid='rejection-reason']"));
        assertThat(reason.getText()).containsIgnoringCase("insufficient funds");
        assertThat(driver.findElements(By.cssSelector("tr[data-trade-id='" + id + "'] button"))).isEmpty();
    }

    @Test
    void fullLifecycleThroughTheUi_endsSettledWithNoFurtherActions() {
        long id = createTrade("BUY", "MSFT", "5", "100.00");
        openDashboard();
        waitForRow(id);

        clickRowButton(id, "validate-btn");
        waitForStatus(id, "VALIDATED");
        clickRowButton(id, "match-btn");
        waitForStatus(id, "MATCHED");
        clickRowButton(id, "settle-btn");
        waitForStatus(id, "SETTLED");

        assertThat(driver.findElements(By.cssSelector("tr[data-trade-id='" + id + "'] button"))).isEmpty();
        assertThat(driver.findElements(By.cssSelector("[data-testid='error-toast']"))).isEmpty();
    }

    @Test
    void clickingCancel_onDraftTrade_movesToCancelled() {
        long id = createTrade("BUY", "AAPL", "1", "10.00");
        openDashboard();
        waitForRow(id);

        clickRowButton(id, "cancel-btn");

        waitForStatus(id, "CANCELLED");
    }

    @Test
    void actionNoLongerAllowed_showsErrorToast_andRowResyncsToRealStatus() {
        long id = createTrade("BUY", "AAPL", "10", "150.00");
        openDashboard();
        waitForRow(id);
        assertThat(statusOf(id)).isEqualTo("DRAFT");

        // Another user validates the trade behind the dashboard's back, so the
        // stale "Validate" button on this page is no longer a legal action.
        apiAction(id, "validate");
        clickRowButton(id, "validate-btn");

        WebElement toast = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("[data-testid='error-toast']")));
        assertThat(toast.getText()).contains("Only DRAFT trades can be validated");
        waitForStatus(id, "VALIDATED");
    }

    @Test
    void errorToast_canBeDismissed() {
        long id = createTrade("BUY", "AAPL", "10", "150.00");
        openDashboard();
        waitForRow(id);
        apiAction(id, "validate");
        clickRowButton(id, "validate-btn");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[data-testid='error-toast']")));

        driver.findElement(By.cssSelector("[data-testid='error-toast'] button")).click();

        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("[data-testid='error-toast']")));
    }

    @Test
    void refreshButton_picksUpTradesCreatedElsewhere() {
        openDashboard();
        long id = createTrade("SELL", "GOOG", "2", "50.00");

        driver.findElement(By.cssSelector("[data-testid='refresh-btn']")).click();

        assertThat(waitForRow(id).getText()).contains("GOOG").contains("SELL");
    }

    @Test
    void newTradeForm_createsDraftTradeInTheTable() {
        openDashboard();
        int rowsBefore = driver.findElements(By.cssSelector("tbody tr[data-trade-id]")).size();
        wait.until(d -> new Select(d.findElement(By.cssSelector("[data-testid='portfolio-select']")))
                .getOptions().size() > 0);
        new Select(driver.findElement(By.cssSelector("[data-testid='portfolio-select']")))
                .selectByValue(String.valueOf(portfolioId));

        driver.findElement(By.cssSelector("[data-testid='ticker-input']")).sendKeys("tsla");
        new Select(driver.findElement(By.cssSelector("[data-testid='type-select']"))).selectByValue("BUY");
        driver.findElement(By.cssSelector("[data-testid='quantity-input']")).sendKeys("3");
        driver.findElement(By.cssSelector("[data-testid='price-input']")).sendKeys("200");
        driver.findElement(By.cssSelector("[data-testid='create-trade-btn']")).click();

        wait.until(d -> d.findElements(By.cssSelector("tbody tr[data-trade-id]")).size() == rowsBefore + 1);
        WebElement newest = driver.findElements(By.cssSelector("tbody tr[data-trade-id]")).stream()
                .filter(r -> r.getText().contains("TSLA")).reduce((a, b) -> b).orElseThrow();
        assertThat(newest.getText()).contains("TSLA").contains("BUY").contains("DRAFT");
        assertThat(driver.findElement(By.cssSelector("[data-testid='ticker-input']")).getAttribute("value")).isEmpty();
    }
}
