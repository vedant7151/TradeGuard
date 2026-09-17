package com.tradeguard.sutapi.repository;

import com.tradeguard.sutapi.entity.Trade;
import com.tradeguard.sutapi.enums.TradeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TradeRepository extends JpaRepository<Trade, Long> {

    List<Trade> findByStatus(TradeStatus status);
}
