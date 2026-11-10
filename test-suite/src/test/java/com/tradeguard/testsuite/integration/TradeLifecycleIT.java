package com.tradeguard.testsuite.integration;

import com.tradeguard.sutapi.entity.Portfolio;
import com.tradeguard.sutapi.entity.Trade;
import com.tradeguard.sutapi.enums.TradeStatus;
import com.tradeguard.sutapi.repository.TradeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises controller -> service -> repository -> real PostgreSQL for the full
 * trade lifecycle and verifies the resulting database state.
 */
class TradeLifecycleIT extends AbstractIntegrationTest {

    @Autowired
    private TradeRepository tradeRepository;

    @Test
    void fullBuyLifecycle_persistsTradeAndUpdatesPositionAndCash() throws Exception {
        long id = createTrade("BUY", "AAPL", "10", "150.00");

        mockMvc.perform(post("/api/trades/{id}/validate", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VALIDATED"));
        mockMvc.perform(post("/api/trades/{id}/match", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MATCHED"));
        mockMvc.perform(post("/api/trades/{id}/settle", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SETTLED"))
                .andExpect(jsonPath("$.settledAt").isNotEmpty());

        Trade persisted = tradeRepository.findById(id).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(TradeStatus.SETTLED);
        assertThat(persisted.getSettledAt()).isNotNull();

        Portfolio updated = portfolioRepository.findById(portfolioId).orElseThrow();
        assertThat(updated.getCashBalance()).isEqualByComparingTo("8500.00");
        assertThat(positionRepository.findByPortfolioIdAndTicker(portfolioId, "AAPL"))
                .isPresent().get()
                .satisfies(p -> assertThat(p.getQuantity()).isEqualByComparingTo("10"));
    }

    @Test
    void fullSellLifecycle_creditsCashAndReducesPosition() throws Exception {
        givenHolding(portfolioId, "AAPL", "20");
        long id = createTrade("SELL", "AAPL", "8", "150.00");

        action(id, "validate");
        action(id, "match");
        mockMvc.perform(post("/api/trades/{id}/settle", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SETTLED"));

        assertThat(portfolioRepository.findById(portfolioId).orElseThrow().getCashBalance()).isEqualByComparingTo("11200");
        assertThat(positionRepository.findByPortfolioIdAndTicker(portfolioId, "AAPL").orElseThrow().getQuantity())
                .isEqualByComparingTo("12");
    }

    @Test
    void insufficientFundsBuy_isRejectedAndCashUntouched() throws Exception {
        long id = createTrade("BUY", "AAPL", "1000", "150.00");

        mockMvc.perform(post("/api/trades/{id}/validate", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason", containsStringIgnoringCase("insufficient funds")));

        assertThat(tradeRepository.findById(id).orElseThrow().getRejectionReason()).isNotBlank();
        assertThat(portfolioRepository.findById(portfolioId).orElseThrow().getCashBalance()).isEqualByComparingTo("10000");
    }

    @Test
    void sellWithoutHolding_isRejected() throws Exception {
        long id = createTrade("SELL", "AAPL", "1", "150.00");

        mockMvc.perform(post("/api/trades/{id}/validate", id))
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason", containsString("Insufficient position")));
    }

    @Test
    void cancellingDraftTrade_marksItCancelledInDatabase() throws Exception {
        long id = createTrade("BUY", "MSFT", "5", "100.00");

        mockMvc.perform(delete("/api/trades/{id}", id)).andExpect(status().isNoContent());

        assertThat(tradeRepository.findById(id).orElseThrow().getStatus()).isEqualTo(TradeStatus.CANCELLED);
    }

    @Test
    void cancellingMatchedTrade_isConflictAndStatusUnchanged() throws Exception {
        long id = createTrade("BUY", "MSFT", "5", "100.00");
        action(id, "validate");
        action(id, "match");

        mockMvc.perform(delete("/api/trades/{id}", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Only DRAFT")));

        assertThat(tradeRepository.findById(id).orElseThrow().getStatus()).isEqualTo(TradeStatus.MATCHED);
    }

    @Test
    void settlingDraftTrade_isConflictAndNothingChanges() throws Exception {
        long id = createTrade("BUY", "MSFT", "5", "100.00");

        mockMvc.perform(post("/api/trades/{id}/settle", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        assertThat(portfolioRepository.findById(portfolioId).orElseThrow().getCashBalance()).isEqualByComparingTo("10000");
        assertThat(positionRepository.findByPortfolioId(portfolioId)).isEmpty();
    }

    @Test
    void settlingTwice_doesNotDoubleApplyEffects() throws Exception {
        long id = createTrade("BUY", "AAPL", "10", "150.00");
        action(id, "validate");
        action(id, "match");
        action(id, "settle");

        mockMvc.perform(post("/api/trades/{id}/settle", id)).andExpect(status().isConflict());

        assertThat(portfolioRepository.findById(portfolioId).orElseThrow().getCashBalance()).isEqualByComparingTo("8500");
        assertThat(positionRepository.findByPortfolioIdAndTicker(portfolioId, "AAPL").orElseThrow().getQuantity())
                .isEqualByComparingTo("10");
    }

    @Test
    void repeatedBuys_accumulateIntoSinglePosition() throws Exception {
        for (int i = 0; i < 2; i++) {
            long id = createTrade("BUY", "AAPL", "3", "100.00");
            action(id, "validate");
            action(id, "match");
            action(id, "settle");
        }

        assertThat(positionRepository.findByPortfolioId(portfolioId)).hasSize(1);
        assertThat(positionRepository.findByPortfolioIdAndTicker(portfolioId, "AAPL").orElseThrow().getQuantity())
                .isEqualByComparingTo("6");
        assertThat(portfolioRepository.findById(portfolioId).orElseThrow().getCashBalance()).isEqualByComparingTo("9400");
    }
}
