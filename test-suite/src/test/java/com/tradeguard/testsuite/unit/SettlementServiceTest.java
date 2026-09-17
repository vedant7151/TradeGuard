package com.tradeguard.testsuite.unit;

import com.tradeguard.sutapi.entity.Portfolio;
import com.tradeguard.sutapi.entity.Position;
import com.tradeguard.sutapi.entity.Trade;
import com.tradeguard.sutapi.enums.TradeType;
import com.tradeguard.sutapi.repository.PortfolioRepository;
import com.tradeguard.sutapi.repository.PositionRepository;
import com.tradeguard.sutapi.service.SettlementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private PositionRepository positionRepository;

    @InjectMocks
    private SettlementService settlementService;

    private Portfolio portfolio;

    @BeforeEach
    void setUp() {
        portfolio = Portfolio.builder().id(1L).name("Test").cashBalance(new BigDecimal("10000")).build();
    }

    @Test
    void settlingBuyTrade_decreasesCashAndIncreasesPosition() {
        Trade trade = Trade.builder()
                .portfolioId(1L).ticker("AAPL").tradeType(TradeType.BUY)
                .quantity(new BigDecimal("10")).price(new BigDecimal("150"))
                .build();

        when(positionRepository.findByPortfolioIdAndTicker(1L, "AAPL")).thenReturn(Optional.empty());

        settlementService.settle(trade, portfolio);

        assertThat(portfolio.getCashBalance()).isEqualByComparingTo("8500");
        verify(positionRepository).save(any(Position.class));
        verify(portfolioRepository).save(portfolio);
    }

    @Test
    void settlingSellTrade_increasesCashAndDecreasesPosition() {
        Trade trade = Trade.builder()
                .portfolioId(1L).ticker("AAPL").tradeType(TradeType.SELL)
                .quantity(new BigDecimal("10")).price(new BigDecimal("150"))
                .build();

        Position existing = Position.builder().portfolioId(1L).ticker("AAPL").quantity(new BigDecimal("20")).build();
        when(positionRepository.findByPortfolioIdAndTicker(1L, "AAPL")).thenReturn(Optional.of(existing));

        settlementService.settle(trade, portfolio);

        assertThat(portfolio.getCashBalance()).isEqualByComparingTo("11500");
        assertThat(existing.getQuantity()).isEqualByComparingTo("10");
    }
}
