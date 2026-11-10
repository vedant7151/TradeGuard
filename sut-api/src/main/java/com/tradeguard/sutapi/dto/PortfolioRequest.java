package com.tradeguard.sutapi.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class PortfolioRequest {

    @NotBlank
    private String name;

    @NotNull
    @DecimalMin("0")
    private BigDecimal cashBalance;

    /** Optional opening holdings (useful for setting up SELL scenarios). */
    @Valid
    private List<PositionRequest> positions;

    @Getter
    @Setter
    public static class PositionRequest {

        @NotBlank
        private String ticker;

        @NotNull
        @DecimalMin(value = "0", inclusive = false)
        private BigDecimal quantity;
    }
}
