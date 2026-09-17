package com.tradeguard.sutapi.controller;

import com.tradeguard.sutapi.dto.PositionResponse;
import com.tradeguard.sutapi.exception.PortfolioNotFoundException;
import com.tradeguard.sutapi.repository.PortfolioRepository;
import com.tradeguard.sutapi.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/portfolios")
@RequiredArgsConstructor
@CrossOrigin
public class PortfolioController {

    private final PortfolioRepository portfolioRepository;
    private final PositionRepository positionRepository;

    @GetMapping("/{id}/positions")
    public List<PositionResponse> getPositions(@PathVariable Long id) {
        portfolioRepository.findById(id).orElseThrow(() -> new PortfolioNotFoundException(id));
        return positionRepository.findByPortfolioId(id).stream()
                .map(PositionResponse::from)
                .toList();
    }
}
