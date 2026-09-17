package com.tradeguard.sutapi.repository;

import com.tradeguard.sutapi.entity.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {
}
