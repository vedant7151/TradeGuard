package com.tradeguard.sutapi.dto;

import com.tradeguard.sutapi.enums.TradeType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class TradeRequest {

    @NotNull
    private Long portfolioId;

    @NotBlank
    private String ticker;

    @NotNull
    private TradeType tradeType;

    @NotNull
    @DecimalMin(value = "0.0001", message = "must be greater than or equal to 0.0001")
    private BigDecimal quantity;

    @NotNull
    @DecimalMin(value = "0.01", message = "must be greater than or equal to 0.01")
    private BigDecimal price;
}
