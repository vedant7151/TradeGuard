package com.tradeguard.testsuite.unit;

import com.tradeguard.sutapi.entity.Portfolio;
import com.tradeguard.sutapi.entity.Position;
import com.tradeguard.sutapi.entity.Trade;
import com.tradeguard.sutapi.enums.TradeType;
import com.tradeguard.sutapi.repository.PositionRepository;
import com.tradeguard.sutapi.service.TradeValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradeValidationServiceTest {

    @Mock
    private PositionRepository positionRepository;

    @InjectMocks
    private TradeValidationService validationService;

    private Portfolio portfolio;

    @BeforeEach
    void setUp() {
        portfolio = Portfolio.builder()
                .id(1L)
                .name("Test Portfolio")
                .cashBalance(new BigDecimal("10000"))
                .build();
    }

    private Trade buyTrade(BigDecimal quantity, BigDecimal price) {
        return Trade.builder()
                .portfolioId(1L)
                .ticker("AAPL")
                .tradeType(TradeType.BUY)
                .quantity(quantity)
                .price(price)
                .build();
    }

    private Trade sellTrade(BigDecimal quantity, BigDecimal price) {
        return Trade.builder()
                .portfolioId(1L)
                .ticker("AAPL")
                .tradeType(TradeType.SELL)
                .quantity(quantity)
                .price(price)
                .build();
    }

    @Test
    void validBuyTradeWithSufficientFunds_isValidated() {
        Trade trade = buyTrade(new BigDecimal("10"), new BigDecimal("150.00"));

        TradeValidationService.ValidationResult result = validationService.validate(trade, portfolio);

        assertThat(result.valid()).isTrue();
        assertThat(result.rejectionReason()).isNull();
    }

    @Test
    void buyTradeWithInsufficientFunds_isRejected() {
        portfolio.setCashBalance(new BigDecimal("500"));
        Trade trade = buyTrade(new BigDecimal("10"), new BigDecimal("150.00"));

        TradeValidationService.ValidationResult result = validationService.validate(trade, portfolio);

        assertThat(result.valid()).isFalse();
        assertThat(result.rejectionReason()).contains("Insufficient funds");
    }

    @Test
    void zeroQuantity_isRejected() {
        Trade trade = buyTrade(BigDecimal.ZERO, new BigDecimal("150.00"));

        TradeValidationService.ValidationResult result = validationService.validate(trade, portfolio);

        assertThat(result.valid()).isFalse();
        assertThat(result.rejectionReason()).contains("Quantity must be greater than zero");
    }

    @Test
    void negativePrice_isRejected() {
        Trade trade = buyTrade(new BigDecimal("10"), new BigDecimal("-1"));

        TradeValidationService.ValidationResult result = validationService.validate(trade, portfolio);

        assertThat(result.valid()).isFalse();
        assertThat(result.rejectionReason()).contains("Price must be greater than zero");
    }

    @Test
    void sellTradeWithSufficientHolding_isValidated() {
        when(positionRepository.findByPortfolioIdAndTicker(1L, "AAPL"))
                .thenReturn(Optional.of(Position.builder().portfolioId(1L).ticker("AAPL").quantity(new BigDecimal("20")).build()));

        Trade trade = sellTrade(new BigDecimal("10"), new BigDecimal("150.00"));

        TradeValidationService.ValidationResult result = validationService.validate(trade, portfolio);

        assertThat(result.valid()).isTrue();
    }

    @Test
    void sellTradeWithNoPosition_isRejected() {
        when(positionRepository.findByPortfolioIdAndTicker(1L, "AAPL")).thenReturn(Optional.empty());

        Trade trade = sellTrade(new BigDecimal("10"), new BigDecimal("150.00"));

        TradeValidationService.ValidationResult result = validationService.validate(trade, portfolio);

        assertThat(result.valid()).isFalse();
        assertThat(result.rejectionReason()).contains("Insufficient position");
    }

    @Test
    void sellTradeWithPartialHolding_isRejected() {
        when(positionRepository.findByPortfolioIdAndTicker(1L, "AAPL"))
                .thenReturn(Optional.of(Position.builder().portfolioId(1L).ticker("AAPL").quantity(new BigDecimal("5")).build()));

        Trade trade = sellTrade(new BigDecimal("10"), new BigDecimal("150.00"));

        TradeValidationService.ValidationResult result = validationService.validate(trade, portfolio);

        assertThat(result.valid()).isFalse();
    }
}
