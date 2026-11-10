package com.tradeguard.testsuite.integration;

import com.tradeguard.sutapi.repository.TradeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Request/response contract of the trade and portfolio endpoints against a real database. */
class TradeApiIT extends AbstractIntegrationTest {

    @Autowired
    private TradeRepository tradeRepository;

    @Test
    void createTrade_returns201WithDraftStatusAndPersistsRow() throws Exception {
        String body = """
                {"portfolioId": %d, "ticker": "AAPL", "tradeType": "BUY", "quantity": 10, "price": 150.00}
                """.formatted(portfolioId);

        String response = mockMvc.perform(post("/api/trades").contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.tradeRef", startsWith("TRD-")))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        long id = objectMapper.readTree(response).get("id").asLong();
        assertThat(tradeRepository.findById(id)).isPresent();
    }

    @Test
    void createTrade_unknownPortfolio_returns404() throws Exception {
        String body = """
                {"portfolioId": 987654, "ticker": "AAPL", "tradeType": "BUY", "quantity": 1, "price": 1}
                """;

        mockMvc.perform(post("/api/trades").contentType("application/json").content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Portfolio not found")));
    }

    @Test
    void createTrade_missingFields_returns400() throws Exception {
        mockMvc.perform(post("/api/trades").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("ticker")))
                .andExpect(jsonPath("$.path").value("/api/trades"));
    }

    @Test
    void createTrade_malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/api/trades").contentType("application/json").content("{not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTrade_unknownTradeType_returns400() throws Exception {
        String body = """
                {"portfolioId": %d, "ticker": "AAPL", "tradeType": "HOLD", "quantity": 1, "price": 1}
                """.formatted(portfolioId);

        mockMvc.perform(post("/api/trades").contentType("application/json").content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTrade_returnsTradeAndUnknownIdReturns404() throws Exception {
        long id = createTrade("BUY", "AAPL", "1", "10");

        mockMvc.perform(get("/api/trades/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticker").value("AAPL"))
                .andExpect(jsonPath("$.tradeType").value("BUY"));

        mockMvc.perform(get("/api/trades/{id}", 987654))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void listTrades_filtersByStatus() throws Exception {
        long draft = createTrade("BUY", "AAPL", "1", "10");
        long validated = createTrade("BUY", "MSFT", "1", "10");
        action(validated, "validate");

        mockMvc.perform(get("/api/trades").param("status", "VALIDATED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].status", everyItem(is("VALIDATED"))))
                .andExpect(jsonPath("$[*].id", hasItem((int) validated)));

        mockMvc.perform(get("/api/trades").param("status", "DRAFT"))
                .andExpect(jsonPath("$[*].id", hasItem((int) draft)));

        mockMvc.perform(get("/api/trades")).andExpect(status().isOk());
    }

    @Test
    void listTrades_unknownStatus_returns400() throws Exception {
        mockMvc.perform(get("/api/trades").param("status", "BOGUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("status")));
    }

    @Test
    void matchingDraftTrade_returns409() throws Exception {
        long id = createTrade("BUY", "AAPL", "1", "10");

        mockMvc.perform(post("/api/trades/{id}/match", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Only VALIDATED")));
    }

    @Test
    void actionsOnUnknownTrade_return404() throws Exception {
        for (String action : new String[]{"validate", "match", "settle"}) {
            mockMvc.perform(post("/api/trades/{id}/" + action, 987654)).andExpect(status().isNotFound());
        }
    }

    // ---------- portfolios ----------

    @Test
    void createPortfolio_withOpeningPositions_persistsThem() throws Exception {
        String body = """
                {"name": "Seeded", "cashBalance": 500, "positions": [{"ticker": "AAPL", "quantity": 4}]}
                """;

        String response = mockMvc.perform(post("/api/portfolios").contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Seeded"))
                .andReturn().getResponse().getContentAsString();

        long id = objectMapper.readTree(response).get("id").asLong();
        mockMvc.perform(get("/api/portfolios/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cashBalance").value(500));
        mockMvc.perform(get("/api/portfolios/{id}/positions", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].ticker").value("AAPL"))
                .andExpect(jsonPath("$[0].quantity").value(4));
    }

    @Test
    void createPortfolio_negativeCash_returns400() throws Exception {
        mockMvc.perform(post("/api/portfolios").contentType("application/json")
                        .content("{\"name\": \"Bad\", \"cashBalance\": -1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownPortfolio_returns404() throws Exception {
        mockMvc.perform(get("/api/portfolios/{id}", 987654)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/portfolios/{id}/positions", 987654)).andExpect(status().isNotFound());
    }

    @Test
    void positions_reflectSettledTrades() throws Exception {
        long id = createTrade("BUY", "AAPL", "10", "150");
        action(id, "validate");
        action(id, "match");
        action(id, "settle");

        mockMvc.perform(get("/api/portfolios/{id}/positions", portfolioId))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].ticker").value("AAPL"))
                .andExpect(jsonPath("$[0].quantity").value(10));
    }
}
