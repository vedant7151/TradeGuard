package com.tradeguard.sutapi.repository;

import com.tradeguard.sutapi.entity.Position;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PositionRepository extends JpaRepository<Position, Long> {

    Optional<Position> findByPortfolioIdAndTicker(Long portfolioId, String ticker);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Position p where p.portfolioId = :portfolioId and p.ticker = :ticker")
    Optional<Position> findByPortfolioIdAndTickerForUpdate(@Param("portfolioId") Long portfolioId, @Param("ticker") String ticker);

    List<Position> findByPortfolioId(Long portfolioId);
}
