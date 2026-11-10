package com.tradeguard.sutapi.service;

import com.tradeguard.sutapi.dto.PortfolioRequest;
import com.tradeguard.sutapi.entity.Portfolio;
import com.tradeguard.sutapi.entity.Position;
import com.tradeguard.sutapi.exception.PortfolioNotFoundException;
import com.tradeguard.sutapi.repository.PortfolioRepository;
import com.tradeguard.sutapi.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final PositionRepository positionRepository;

    @Transactional
    public Portfolio createPortfolio(PortfolioRequest request) {
        Portfolio portfolio = portfolioRepository.save(Portfolio.builder()
                .name(request.getName())
                .cashBalance(request.getCashBalance())
                .build());

        if (request.getPositions() != null) {
            for (PortfolioRequest.PositionRequest p : request.getPositions()) {
                positionRepository.save(Position.builder()
                        .portfolioId(portfolio.getId())
                        .ticker(p.getTicker())
                        .quantity(p.getQuantity())
                        .build());
            }
        }
        return portfolio;
    }

    public Portfolio getPortfolio(Long id) {
        return portfolioRepository.findById(id).orElseThrow(() -> new PortfolioNotFoundException(id));
    }

    public List<Portfolio> listPortfolios() {
        return portfolioRepository.findAll();
    }

    public List<Position> getPositions(Long portfolioId) {
        getPortfolio(portfolioId);
        return positionRepository.findByPortfolioId(portfolioId);
    }
}
