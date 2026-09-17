package com.tradeguard.sutapi.dto;

import com.tradeguard.sutapi.enums.TradeType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class TradeRequest {

    @NotNull
    private Long portfolioId;

    @NotNull
    private String ticker;

    @NotNull
    private TradeType tradeType;

    @NotNull
    private BigDecimal quantity;

    @NotNull
    private BigDecimal price;
}
