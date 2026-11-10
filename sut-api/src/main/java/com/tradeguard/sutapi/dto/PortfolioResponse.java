package com.tradeguard.sutapi.dto;

import com.tradeguard.sutapi.entity.Portfolio;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class PortfolioResponse {

    private Long id;
    private String name;
    private BigDecimal cashBalance;

    public static PortfolioResponse from(Portfolio portfolio) {
        return PortfolioResponse.builder()
                .id(portfolio.getId())
                .name(portfolio.getName())
                .cashBalance(portfolio.getCashBalance())
                .build();
    }
}
