package com.tradeguard.testsuite.integration;

import com.tradeguard.sutapi.entity.Portfolio;
import com.tradeguard.sutapi.entity.Position;
import com.tradeguard.sutapi.entity.Trade;
import com.tradeguard.sutapi.enums.TradeStatus;
import com.tradeguard.sutapi.repository.TradeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Verifies that concurrent settlement attempts against the same MATCHED trade
 * are serialized by pessimistic locking: exactly one succeeds (200 OK) and the
 * other is rejected as a conflict (409 Conflict), preventing double settlement.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ConcurrentSettlementIT extends AbstractIntegrationTest {

    @Autowired
    private TradeRepository tradeRepository;

    @Test
    void concurrentSettlement_resultsInExactlyOneSuccessAndOneConflict() throws Exception {
        // 1. Get the trade to MATCHED status against the default portfolio ($10,000 cash)
        long id = createTrade("BUY", "AAPL", "10", "150.00");
        action(id, "validate");
        action(id, "match");

        // 2. Fire two concurrent HTTP POST /api/trades/{id}/settle requests at the SAME trade id,
        // synchronized with CountDownLatch so both threads are ready before either calls settle.
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> settleTask = () -> {
                readyLatch.countDown();
                startLatch.await();
                return mockMvc.perform(post("/api/trades/{id}/settle", id))
                        .andReturn()
                        .getResponse()
                        .getStatus();
            };

            Future<Integer> future1 = executor.submit(settleTask);
            Future<Integer> future2 = executor.submit(settleTask);

            // Wait until both threads are primed and waiting on startLatch
            assertThat(readyLatch.await(5, TimeUnit.SECONDS)).isTrue();
            // Release both threads simultaneously
            startLatch.countDown();

            int status1 = future1.get(10, TimeUnit.SECONDS);
            int status2 = future2.get(10, TimeUnit.SECONDS);

            // 3. Assert the two response statuses are exactly one 200 and one 409 (in either order)
            assertThat(List.of(status1, status2)).containsExactlyInAnyOrder(200, 409);

            // 4. Assert the final persisted Trade status is SETTLED (not corrupted)
            Trade persisted = tradeRepository.findById(id).orElseThrow();
            assertThat(persisted.getStatus()).isEqualTo(TradeStatus.SETTLED);

            // 5. Assert the Portfolio's cash balance reflects the settlement applied exactly ONCE
            // (starting $10,000 - 10 * $150 = $8,500, not $7,000 which would indicate double-application)
            Portfolio updated = portfolioRepository.findById(portfolioId).orElseThrow();
            assertThat(updated.getCashBalance()).isEqualByComparingTo("8500.00");

            // 6. Assert the resulting Position quantity reflects the trade applied exactly once (quantity == 10, not 20)
            Position position = positionRepository.findByPortfolioIdAndTicker(portfolioId, "AAPL").orElseThrow();
            assertThat(position.getQuantity()).isEqualByComparingTo("10");
        } finally {
            executor.shutdownNow();
        }
    }
}
