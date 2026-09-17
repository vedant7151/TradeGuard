package com.tradeguard.testsuite.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradeguard.sutapi.dto.TradeRequest;
import com.tradeguard.sutapi.entity.Portfolio;
import com.tradeguard.sutapi.enums.TradeType;
import com.tradeguard.sutapi.repository.PortfolioRepository;
import com.tradeguard.sutapi.repository.PositionRepository;
import com.tradeguard.sutapi.repository.TradeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Exercises the full controller -> service -> repository -> real PostgreSQL
 * stack for the complete trade lifecycle: DRAFT -> VALIDATED -> MATCHED -> SETTLED.
 */
@Transactional
class TradeLifecycleIT extends AbstractIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private TradeRepository tradeRepository;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Long portfolioId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        Portfolio portfolio = portfolioRepository.save(
                Portfolio.builder().name("IT Portfolio").cashBalance(new BigDecimal("10000")).build());
        portfolioId = portfolio.getId();
    }

    @Test
    void fullBuyTradeLifecycle_updatesPositionAndCashInDatabase() throws Exception {
        TradeRequest request = new TradeRequest();
        request.setPortfolioId(portfolioId);
        request.setTicker("AAPL");
        request.setTradeType(TradeType.BUY);
        request.setQuantity(new BigDecimal("10"));
        request.setPrice(new BigDecimal("150.00"));

        String createResponse = mockMvc.perform(post("/api/trades")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn().getResponse().getContentAsString();

        Long tradeId = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(post("/api/trades/{id}/validate", tradeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VALIDATED"));

        mockMvc.perform(post("/api/trades/{id}/match", tradeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MATCHED"));

        mockMvc.perform(post("/api/trades/{id}/settle", tradeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SETTLED"));

        Portfolio updated = portfolioRepository.findById(portfolioId).orElseThrow();
        assertThat(updated.getCashBalance()).isEqualByComparingTo("8500.00");

        assertThat(positionRepository.findByPortfolioIdAndTicker(portfolioId, "AAPL"))
                .isPresent()
                .get()
                .satisfies(position -> assertThat(position.getQuantity()).isEqualByComparingTo("10"));
    }

    @Test
    void insufficientFundsBuyTrade_isRejected() throws Exception {
        TradeRequest request = new TradeRequest();
        request.setPortfolioId(portfolioId);
        request.setTicker("AAPL");
        request.setTradeType(TradeType.BUY);
        request.setQuantity(new BigDecimal("1000"));
        request.setPrice(new BigDecimal("150.00"));

        String createResponse = mockMvc.perform(post("/api/trades")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        Long tradeId = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(post("/api/trades/{id}/validate", tradeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason", containsStringIgnoringCase("insufficient funds")));
    }

    @Test
    void cancellingDraftTrade_removesItFromActiveList() throws Exception {
        TradeRequest request = new TradeRequest();
        request.setPortfolioId(portfolioId);
        request.setTicker("MSFT");
        request.setTradeType(TradeType.BUY);
        request.setQuantity(new BigDecimal("5"));
        request.setPrice(new BigDecimal("100.00"));

        String createResponse = mockMvc.perform(post("/api/trades")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        Long tradeId = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(delete("/api/trades/{id}", tradeId)).andExpect(status().isNoContent());

        assertThat(tradeRepository.findById(tradeId).orElseThrow().getStatus().name()).isEqualTo("CANCELLED");
    }
}
