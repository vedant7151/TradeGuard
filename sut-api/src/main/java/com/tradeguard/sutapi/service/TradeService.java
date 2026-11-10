package com.tradeguard.sutapi.service;

import com.tradeguard.sutapi.dto.TradeRequest;
import com.tradeguard.sutapi.entity.Portfolio;
import com.tradeguard.sutapi.entity.Trade;
import com.tradeguard.sutapi.enums.TradeStatus;
import com.tradeguard.sutapi.exception.InvalidTradeStateException;
import com.tradeguard.sutapi.exception.PortfolioNotFoundException;
import com.tradeguard.sutapi.exception.TradeNotFoundException;
import com.tradeguard.sutapi.repository.PortfolioRepository;
import com.tradeguard.sutapi.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TradeService {

    private final TradeRepository tradeRepository;
    private final PortfolioRepository portfolioRepository;
    private final TradeValidationService tradeValidationService;
    private final SettlementService settlementService;

    @Transactional
    public Trade createTrade(TradeRequest request) {
        portfolioRepository.findById(request.getPortfolioId())
                .orElseThrow(() -> new PortfolioNotFoundException(request.getPortfolioId()));

        Trade trade = Trade.builder()
                .tradeRef("TRD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .portfolioId(request.getPortfolioId())
                .ticker(request.getTicker())
                .tradeType(request.getTradeType())
                .quantity(request.getQuantity())
                .price(request.getPrice())
                .status(TradeStatus.DRAFT)
                .createdAt(Instant.now())
                .build();

        return tradeRepository.save(trade);
    }

    public Trade getTrade(Long id) {
        return tradeRepository.findById(id).orElseThrow(() -> new TradeNotFoundException(id));
    }

    public List<Trade> listTrades(TradeStatus status) {
        return status == null ? tradeRepository.findAll() : tradeRepository.findByStatus(status);
    }

    @Transactional
    public Trade validateTrade(Long id) {
        Trade trade = getTrade(id);
        if (trade.getStatus() != TradeStatus.DRAFT) {
            throw new InvalidTradeStateException("Only DRAFT trades can be validated, current status: " + trade.getStatus());
        }

        Long portfolioId = trade.getPortfolioId();
        Portfolio portfolio = portfolioRepository.findByIdForUpdate(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));

        TradeValidationService.ValidationResult result = tradeValidationService.validate(trade, portfolio);

        if (result.valid()) {
            trade.setStatus(TradeStatus.VALIDATED);
            trade.setRejectionReason(null);
        } else {
            trade.setStatus(TradeStatus.REJECTED);
            trade.setRejectionReason(result.rejectionReason());
        }

        return tradeRepository.save(trade);
    }

    @Transactional
    public Trade matchTrade(Long id) {
        Trade trade = getTrade(id);
        if (trade.getStatus() != TradeStatus.VALIDATED) {
            throw new InvalidTradeStateException("Only VALIDATED trades can be matched, current status: " + trade.getStatus());
        }
        trade.setStatus(TradeStatus.MATCHED);
        return tradeRepository.save(trade);
    }

    @Transactional
    public Trade settleTrade(Long id) {
        Trade trade = getTrade(id);
        if (trade.getStatus() != TradeStatus.MATCHED) {
            throw new InvalidTradeStateException("Only MATCHED trades can be settled, current status: " + trade.getStatus());
        }

        Long portfolioId = trade.getPortfolioId();
        Portfolio portfolio = portfolioRepository.findByIdForUpdate(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));

        trade = getTrade(id);
        if (trade.getStatus() != TradeStatus.MATCHED) {
            throw new InvalidTradeStateException("Only MATCHED trades can be settled, current status: " + trade.getStatus());
        }

        settlementService.settle(trade, portfolio);

        trade.setStatus(TradeStatus.SETTLED);
        trade.setSettledAt(Instant.now());
        return tradeRepository.save(trade);
    }

    @Transactional
    public void cancelTrade(Long id) {
        Trade trade = getTrade(id);
        if (trade.getStatus() != TradeStatus.DRAFT) {
            throw new InvalidTradeStateException("Only DRAFT trades can be cancelled, current status: " + trade.getStatus());
        }
        trade.setStatus(TradeStatus.CANCELLED);
        tradeRepository.save(trade);
    }
}
