package com.tradeguard.sutapi.config;

import com.tradeguard.sutapi.entity.Portfolio;
import com.tradeguard.sutapi.repository.PortfolioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Seeds a demo portfolio on an empty database so the API and dashboard are
 * usable immediately after start-up (portable across PostgreSQL and H2).
 */
@Component
public class DemoDataInitializer implements ApplicationRunner {

    private final PortfolioRepository portfolioRepository;
    private final boolean enabled;
    private final BigDecimal cash;

    public DemoDataInitializer(PortfolioRepository portfolioRepository,
                               @Value("${tradeguard.seed-demo-data:true}") boolean enabled,
                               @Value("${tradeguard.demo-cash-balance:100000}") BigDecimal cash) {
        this.portfolioRepository = portfolioRepository;
        this.enabled = enabled;
        this.cash = cash;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (enabled && portfolioRepository.count() == 0) {
            portfolioRepository.save(Portfolio.builder().name("Demo Portfolio").cashBalance(cash).build());
        }
    }
}
