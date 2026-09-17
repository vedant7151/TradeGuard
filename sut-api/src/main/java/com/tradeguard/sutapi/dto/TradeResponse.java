package com.tradeguard.sutapi.dto;

import com.tradeguard.sutapi.entity.Trade;
import com.tradeguard.sutapi.enums.TradeStatus;
import com.tradeguard.sutapi.enums.TradeType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Builder
public class TradeResponse {

    private Long id;
    private String tradeRef;
    private Long portfolioId;
    private String ticker;
    private TradeType tradeType;
    private BigDecimal quantity;
    private BigDecimal price;
    private TradeStatus status;
    private String rejectionReason;
    private Instant createdAt;
    private Instant settledAt;

    public static TradeResponse from(Trade trade) {
        return TradeResponse.builder()
                .id(trade.getId())
                .tradeRef(trade.getTradeRef())
                .portfolioId(trade.getPortfolioId())
                .ticker(trade.getTicker())
                .tradeType(trade.getTradeType())
                .quantity(trade.getQuantity())
                .price(trade.getPrice())
                .status(trade.getStatus())
                .rejectionReason(trade.getRejectionReason())
                .createdAt(trade.getCreatedAt())
                .settledAt(trade.getSettledAt())
                .build();
    }
}
