package com.tradeguard.sutapi.dto;

import com.tradeguard.sutapi.entity.Position;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class PositionResponse {

    private Long portfolioId;
    private String ticker;
    private BigDecimal quantity;

    public static PositionResponse from(Position position) {
        return PositionResponse.builder()
                .portfolioId(position.getPortfolioId())
                .ticker(position.getTicker())
                .quantity(position.getQuantity())
                .build();
    }
}
