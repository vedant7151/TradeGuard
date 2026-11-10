package com.tradeguard.testsuite.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradeguard.sutapi.TradeGuardApiApplication;
import com.tradeguard.sutapi.entity.Portfolio;
import com.tradeguard.sutapi.entity.Position;
import com.tradeguard.sutapi.repository.PortfolioRepository;
import com.tradeguard.sutapi.repository.PositionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Base class that spins up a real PostgreSQL container shared by all integration
 * tests, so tests run against the same database engine used in production rather
 * than an in-memory substitute. Each test runs in a transaction that is rolled
 * back afterwards, keeping tests independent.
 */
@Transactional
@SpringBootTest(classes = TradeGuardApiApplication.class)
public abstract class AbstractIntegrationTest {

    // Singleton container: started once for the whole test JVM and shared by every IT class.
    // (A per-class @Container would be stopped after the first class while Spring's cached
    // context still points at it.) Testcontainers' Ryuk sidecar removes it on JVM exit.
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("tradeguard_test")
            .withUsername("tradeguard")
            .withPassword("tradeguard");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void registerDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired protected WebApplicationContext webApplicationContext;
    @Autowired protected PortfolioRepository portfolioRepository;
    @Autowired protected PositionRepository positionRepository;

    protected final ObjectMapper objectMapper = new ObjectMapper();
    protected MockMvc mockMvc;
    protected Long portfolioId;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        portfolioId = newPortfolio("10000").getId();
    }

    protected Portfolio newPortfolio(String cash) {
        return portfolioRepository.save(Portfolio.builder().name("IT Portfolio").cashBalance(new BigDecimal(cash)).build());
    }

    protected void givenHolding(Long portfolio, String ticker, String quantity) {
        positionRepository.save(Position.builder()
                .portfolioId(portfolio).ticker(ticker).quantity(new BigDecimal(quantity)).build());
    }

    /** Creates a trade through the API and returns its id. */
    protected long createTrade(String type, String ticker, String quantity, String price) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "portfolioId", portfolioId, "ticker", ticker, "tradeType", type,
                "quantity", new BigDecimal(quantity), "price", new BigDecimal(price)));
        String response = mockMvc.perform(post("/api/trades").contentType("application/json").content(body))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    protected void action(long tradeId, String action) throws Exception {
        mockMvc.perform(post("/api/trades/{id}/" + action, tradeId));
    }
}
