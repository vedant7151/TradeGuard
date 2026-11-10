package com.tradeguard.testsuite.unit;

import com.tradeguard.sutapi.dto.TradeRequest;
import com.tradeguard.sutapi.entity.Portfolio;
import com.tradeguard.sutapi.entity.Trade;
import com.tradeguard.sutapi.enums.TradeStatus;
import com.tradeguard.sutapi.enums.TradeType;
import com.tradeguard.sutapi.exception.InvalidTradeStateException;
import com.tradeguard.sutapi.exception.PortfolioNotFoundException;
import com.tradeguard.sutapi.exception.TradeNotFoundException;
import com.tradeguard.sutapi.repository.PortfolioRepository;
import com.tradeguard.sutapi.repository.TradeRepository;
import com.tradeguard.sutapi.service.SettlementService;
import com.tradeguard.sutapi.service.TradeService;
import com.tradeguard.sutapi.service.TradeValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradeServiceTest {

    @Mock private TradeRepository tradeRepository;
    @Mock private PortfolioRepository portfolioRepository;
    @Mock private TradeValidationService validationService;
    @Mock private SettlementService settlementService;

    @InjectMocks private TradeService tradeService;

    private Portfolio portfolio;

    @BeforeEach
    void setUp() {
        portfolio = Portfolio.builder().id(1L).name("P").cashBalance(new BigDecimal("10000")).build();
    }

    private Trade tradeWithStatus(TradeStatus status) {
        return Trade.builder().id(7L).portfolioId(1L).ticker("AAPL").tradeType(TradeType.BUY)
                .quantity(new BigDecimal("10")).price(new BigDecimal("150")).status(status).build();
    }

    private void stubFind(Trade trade) {
        when(tradeRepository.findById(7L)).thenReturn(Optional.of(trade));
    }

    // ---------- create ----------

    @Test
    void createTrade_persistsDraftWithGeneratedReference() {
        TradeRequest request = new TradeRequest();
        request.setPortfolioId(1L);
        request.setTicker("AAPL");
        request.setTradeType(TradeType.BUY);
        request.setQuantity(new BigDecimal("10"));
        request.setPrice(new BigDecimal("150"));
        when(portfolioRepository.findById(1L)).thenReturn(Optional.of(portfolio));
        when(tradeRepository.save(any(Trade.class))).thenAnswer(inv -> inv.getArgument(0));

        Trade saved = tradeService.createTrade(request);

        assertThat(saved.getStatus()).isEqualTo(TradeStatus.DRAFT);
        assertThat(saved.getTradeRef()).startsWith("TRD-");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getSettledAt()).isNull();
    }

    @Test
    void createTrade_unknownPortfolio_throws() {
        TradeRequest request = new TradeRequest();
        request.setPortfolioId(99L);
        when(portfolioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tradeService.createTrade(request)).isInstanceOf(PortfolioNotFoundException.class);
        verify(tradeRepository, never()).save(any());
    }

    // ---------- read ----------

    @Test
    void getTrade_unknownId_throws() {
        when(tradeRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tradeService.getTrade(7L))
                .isInstanceOf(TradeNotFoundException.class)
                .hasMessageContaining("7");
    }

    @Test
    void listTrades_withoutFilter_returnsAll() {
        when(tradeRepository.findAll()).thenReturn(List.of(tradeWithStatus(TradeStatus.DRAFT)));

        assertThat(tradeService.listTrades(null)).hasSize(1);
        verify(tradeRepository, never()).findByStatus(any());
    }

    @Test
    void listTrades_withFilter_delegatesToStatusQuery() {
        when(tradeRepository.findByStatus(TradeStatus.MATCHED)).thenReturn(List.of());

        assertThat(tradeService.listTrades(TradeStatus.MATCHED)).isEmpty();
        verify(tradeRepository, never()).findAll();
    }

    // ---------- validate ----------

    @Test
    void validateTrade_validResult_movesToValidated() {
        Trade trade = tradeWithStatus(TradeStatus.DRAFT);
        stubFind(trade);
        when(portfolioRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(portfolio));
        when(validationService.validate(trade, portfolio)).thenReturn(TradeValidationService.ValidationResult.ok());
        when(tradeRepository.save(trade)).thenReturn(trade);

        Trade result = tradeService.validateTrade(7L);

        assertThat(result.getStatus()).isEqualTo(TradeStatus.VALIDATED);
        assertThat(result.getRejectionReason()).isNull();
    }

    @Test
    void validateTrade_invalidResult_movesToRejectedWithReason() {
        Trade trade = tradeWithStatus(TradeStatus.DRAFT);
        stubFind(trade);
        when(portfolioRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(portfolio));
        when(validationService.validate(trade, portfolio))
                .thenReturn(TradeValidationService.ValidationResult.rejected("Insufficient funds"));
        when(tradeRepository.save(trade)).thenReturn(trade);

        Trade result = tradeService.validateTrade(7L);

        assertThat(result.getStatus()).isEqualTo(TradeStatus.REJECTED);
        assertThat(result.getRejectionReason()).isEqualTo("Insufficient funds");
    }

    @Test
    void validateTrade_missingPortfolio_throws() {
        stubFind(tradeWithStatus(TradeStatus.DRAFT));
        when(portfolioRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tradeService.validateTrade(7L)).isInstanceOf(PortfolioNotFoundException.class);
    }

    @ParameterizedTest
    @EnumSource(value = TradeStatus.class, names = "DRAFT", mode = EnumSource.Mode.EXCLUDE)
    void validateTrade_onlyDraftAllowed(TradeStatus status) {
        stubFind(tradeWithStatus(status));

        assertThatThrownBy(() -> tradeService.validateTrade(7L))
                .isInstanceOf(InvalidTradeStateException.class)
                .hasMessageContaining("Only DRAFT");
        verifyNoInteractions(validationService);
    }

    // ---------- match ----------

    @Test
    void matchTrade_validatedTrade_movesToMatched() {
        Trade trade = tradeWithStatus(TradeStatus.VALIDATED);
        stubFind(trade);
        when(tradeRepository.save(trade)).thenReturn(trade);

        assertThat(tradeService.matchTrade(7L).getStatus()).isEqualTo(TradeStatus.MATCHED);
    }

    @ParameterizedTest
    @EnumSource(value = TradeStatus.class, names = "VALIDATED", mode = EnumSource.Mode.EXCLUDE)
    void matchTrade_onlyValidatedAllowed(TradeStatus status) {
        stubFind(tradeWithStatus(status));

        assertThatThrownBy(() -> tradeService.matchTrade(7L))
                .isInstanceOf(InvalidTradeStateException.class)
                .hasMessageContaining("Only VALIDATED");
        verify(tradeRepository, never()).save(any());
    }

    // ---------- settle ----------

    @Test
    void settleTrade_matchedTrade_settlesAndStampsTime() {
        Trade trade = tradeWithStatus(TradeStatus.MATCHED);
        stubFind(trade);
        when(portfolioRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(portfolio));
        when(tradeRepository.save(trade)).thenReturn(trade);

        Trade result = tradeService.settleTrade(7L);

        verify(settlementService).settle(trade, portfolio);
        assertThat(result.getStatus()).isEqualTo(TradeStatus.SETTLED);
        assertThat(result.getSettledAt()).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(value = TradeStatus.class, names = "MATCHED", mode = EnumSource.Mode.EXCLUDE)
    void settleTrade_onlyMatchedAllowed(TradeStatus status) {
        stubFind(tradeWithStatus(status));

        assertThatThrownBy(() -> tradeService.settleTrade(7L))
                .isInstanceOf(InvalidTradeStateException.class)
                .hasMessageContaining("Only MATCHED");
        verifyNoInteractions(settlementService);
    }

    @Test
    void settleTrade_missingPortfolio_throwsAndDoesNotSettle() {
        stubFind(tradeWithStatus(TradeStatus.MATCHED));
        when(portfolioRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tradeService.settleTrade(7L)).isInstanceOf(PortfolioNotFoundException.class);
        verifyNoInteractions(settlementService);
    }

    // ---------- cancel ----------

    @Test
    void cancelTrade_draftTrade_movesToCancelled() {
        Trade trade = tradeWithStatus(TradeStatus.DRAFT);
        stubFind(trade);

        tradeService.cancelTrade(7L);

        ArgumentCaptor<Trade> captor = ArgumentCaptor.forClass(Trade.class);
        verify(tradeRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(TradeStatus.CANCELLED);
    }

    @ParameterizedTest
    @EnumSource(value = TradeStatus.class, names = "DRAFT", mode = EnumSource.Mode.EXCLUDE)
    void cancelTrade_onlyDraftAllowed(TradeStatus status) {
        stubFind(tradeWithStatus(status));

        assertThatThrownBy(() -> tradeService.cancelTrade(7L))
                .isInstanceOf(InvalidTradeStateException.class)
                .hasMessageContaining("Only DRAFT");
        verify(tradeRepository, never()).save(any());
    }
}
