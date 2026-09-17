package com.tradeguard.sutapi.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "positions", uniqueConstraints = @UniqueConstraint(columnNames = {"portfolio_id", "ticker"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "portfolio_id", nullable = false)
    private Long portfolioId;

    @Column(nullable = false)
    private String ticker;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;
}
