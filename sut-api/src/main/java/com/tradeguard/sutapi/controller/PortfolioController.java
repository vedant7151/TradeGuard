package com.tradeguard.sutapi.controller;

import com.tradeguard.sutapi.dto.PortfolioRequest;
import com.tradeguard.sutapi.dto.PortfolioResponse;
import com.tradeguard.sutapi.dto.PositionResponse;
import com.tradeguard.sutapi.service.PortfolioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/portfolios")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;

    @PostMapping
    public ResponseEntity<PortfolioResponse> createPortfolio(@Valid @RequestBody PortfolioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PortfolioResponse.from(portfolioService.createPortfolio(request)));
    }

    @GetMapping
    public List<PortfolioResponse> listPortfolios() {
        return portfolioService.listPortfolios().stream().map(PortfolioResponse::from).toList();
    }

    @GetMapping("/{id}")
    public PortfolioResponse getPortfolio(@PathVariable Long id) {
        return PortfolioResponse.from(portfolioService.getPortfolio(id));
    }

    @GetMapping("/{id}/positions")
    public List<PositionResponse> getPositions(@PathVariable Long id) {
        return portfolioService.getPositions(id).stream().map(PositionResponse::from).toList();
    }
}
