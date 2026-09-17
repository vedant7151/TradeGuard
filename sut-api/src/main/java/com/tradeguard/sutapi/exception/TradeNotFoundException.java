package com.tradeguard.sutapi.exception;

public class TradeNotFoundException extends RuntimeException {

    public TradeNotFoundException(Long id) {
        super("Trade not found with id: " + id);
    }
}
