package com.tradeguard.sutapi.service;

import com.tradeguard.sutapi.entity.Portfolio;
import com.tradeguard.sutapi.entity.Position;
import com.tradeguard.sutapi.entity.Trade;
import com.tradeguard.sutapi.enums.TradeType;
import com.tradeguard.sutapi.exception.InsufficientFundsException;
import com.tradeguard.sutapi.exception.InsufficientPositionException;
import com.tradeguard.sutapi.repository.PortfolioRepository;
import com.tradeguard.sutapi.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Applies the cash and position effects of a MATCHED trade moving to SETTLED.
 * BUY: cash decreases, position quantity increases.
 * SELL: cash increases, position quantity decreases.
 */
@Service
@RequiredArgsConstructor
public class SettlementService {

    private final PortfolioRepository portfolioRepository;
    private final PositionRepository positionRepository;

    public void settle(Trade trade, Portfolio portfolio) {
        BigDecimal notional = trade.getQuantity().multiply(trade.getPrice());

        Position position = positionRepository
                .findByPortfolioIdAndTickerForUpdate(trade.getPortfolioId(), trade.getTicker())
                .orElseGet(() -> Position.builder()
                        .portfolioId(trade.getPortfolioId())
                        .ticker(trade.getTicker())
                        .quantity(BigDecimal.ZERO)
                        .build());

        if (trade.getTradeType() == TradeType.BUY) {
            if (portfolio.getCashBalance().compareTo(notional) < 0) {
                throw new InsufficientFundsException("Insufficient funds: required " + notional
                        + " but available " + portfolio.getCashBalance());
            }
            portfolio.setCashBalance(portfolio.getCashBalance().subtract(notional));
            position.setQuantity(position.getQuantity().add(trade.getQuantity()));
        } else {
            if (position.getQuantity().compareTo(trade.getQuantity()) < 0) {
                throw new InsufficientPositionException("Insufficient position: held " + position.getQuantity()
                        + " but requested " + trade.getQuantity());
            }
            portfolio.setCashBalance(portfolio.getCashBalance().add(notional));
            position.setQuantity(position.getQuantity().subtract(trade.getQuantity()));
        }

        portfolioRepository.save(portfolio);
        positionRepository.save(position);
    }
}
