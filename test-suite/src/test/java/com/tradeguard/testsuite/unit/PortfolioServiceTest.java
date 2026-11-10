package com.tradeguard.testsuite.unit;

import com.tradeguard.sutapi.dto.PortfolioRequest;
import com.tradeguard.sutapi.entity.Portfolio;
import com.tradeguard.sutapi.entity.Position;
import com.tradeguard.sutapi.exception.PortfolioNotFoundException;
import com.tradeguard.sutapi.repository.PortfolioRepository;
import com.tradeguard.sutapi.repository.PositionRepository;
import com.tradeguard.sutapi.service.PortfolioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock private PortfolioRepository portfolioRepository;
    @Mock private PositionRepository positionRepository;

    @InjectMocks private PortfolioService portfolioService;

    private PortfolioRequest request(String name, String cash) {
        PortfolioRequest r = new PortfolioRequest();
        r.setName(name);
        r.setCashBalance(new BigDecimal(cash));
        return r;
    }

    @Test
    void createPortfolio_withoutPositions_savesOnlyPortfolio() {
        when(portfolioRepository.save(any(Portfolio.class))).thenAnswer(inv -> {
            Portfolio p = inv.getArgument(0);
            p.setId(5L);
            return p;
        });

        Portfolio created = portfolioService.createPortfolio(request("Alpha", "1000"));

        assertThat(created.getId()).isEqualTo(5L);
        assertThat(created.getCashBalance()).isEqualByComparingTo("1000");
        verify(positionRepository, never()).save(any());
    }

    @Test
    void createPortfolio_withOpeningPositions_savesEachPosition() {
        PortfolioRequest r = request("Beta", "0");
        PortfolioRequest.PositionRequest pos = new PortfolioRequest.PositionRequest();
        pos.setTicker("AAPL");
        pos.setQuantity(new BigDecimal("20"));
        r.setPositions(List.of(pos));
        when(portfolioRepository.save(any(Portfolio.class))).thenAnswer(inv -> {
            Portfolio p = inv.getArgument(0);
            p.setId(6L);
            return p;
        });

        portfolioService.createPortfolio(r);

        ArgumentCaptor<Position> captor = ArgumentCaptor.forClass(Position.class);
        verify(positionRepository).save(captor.capture());
        assertThat(captor.getValue().getPortfolioId()).isEqualTo(6L);
        assertThat(captor.getValue().getTicker()).isEqualTo("AAPL");
        assertThat(captor.getValue().getQuantity()).isEqualByComparingTo("20");
    }

    @Test
    void getPortfolio_unknown_throws() {
        when(portfolioRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> portfolioService.getPortfolio(9L)).isInstanceOf(PortfolioNotFoundException.class);
    }

    @Test
    void listPortfolios_returnsAll() {
        when(portfolioRepository.findAll()).thenReturn(List.of(new Portfolio(), new Portfolio()));

        assertThat(portfolioService.listPortfolios()).hasSize(2);
    }

    @Test
    void getPositions_knownPortfolio_returnsPositions() {
        when(portfolioRepository.findById(1L)).thenReturn(Optional.of(new Portfolio()));
        when(positionRepository.findByPortfolioId(1L)).thenReturn(List.of(new Position()));

        assertThat(portfolioService.getPositions(1L)).hasSize(1);
    }

    @Test
    void getPositions_unknownPortfolio_throws() {
        when(portfolioRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> portfolioService.getPositions(1L)).isInstanceOf(PortfolioNotFoundException.class);
        verify(positionRepository, never()).findByPortfolioId(any());
    }
}
