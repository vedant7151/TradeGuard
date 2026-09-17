package com.tradeguard.sutapi.controller;

import com.tradeguard.sutapi.dto.TradeRequest;
import com.tradeguard.sutapi.dto.TradeResponse;
import com.tradeguard.sutapi.entity.Trade;
import com.tradeguard.sutapi.enums.TradeStatus;
import com.tradeguard.sutapi.service.TradeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
@CrossOrigin
public class TradeController {

    private final TradeService tradeService;

    @PostMapping
    public ResponseEntity<TradeResponse> createTrade(@Valid @RequestBody TradeRequest request) {
        Trade trade = tradeService.createTrade(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(TradeResponse.from(trade));
    }

    @GetMapping("/{id}")
    public TradeResponse getTrade(@PathVariable Long id) {
        return TradeResponse.from(tradeService.getTrade(id));
    }

    @GetMapping
    public List<TradeResponse> listTrades(@RequestParam(required = false) TradeStatus status) {
        return tradeService.listTrades(status).stream().map(TradeResponse::from).toList();
    }

    @PostMapping("/{id}/validate")
    public TradeResponse validateTrade(@PathVariable Long id) {
        return TradeResponse.from(tradeService.validateTrade(id));
    }

    @PostMapping("/{id}/match")
    public TradeResponse matchTrade(@PathVariable Long id) {
        return TradeResponse.from(tradeService.matchTrade(id));
    }

    @PostMapping("/{id}/settle")
    public TradeResponse settleTrade(@PathVariable Long id) {
        return TradeResponse.from(tradeService.settleTrade(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelTrade(@PathVariable Long id) {
        tradeService.cancelTrade(id);
        return ResponseEntity.noContent().build();
    }
}
