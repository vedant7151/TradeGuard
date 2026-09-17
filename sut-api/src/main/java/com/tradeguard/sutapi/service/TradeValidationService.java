package com.tradeguard.sutapi.service;

import com.tradeguard.sutapi.entity.Portfolio;
import com.tradeguard.sutapi.entity.Position;
import com.tradeguard.sutapi.entity.Trade;
import com.tradeguard.sutapi.enums.TradeType;
import com.tradeguard.sutapi.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Encapsulates the business rules that decide whether a DRAFT trade may
 * move to VALIDATED or must be REJECTED. Kept side-effect free (no
 * persistence) so it can be unit tested in isolation with mocked repos.
 */
@Service
@RequiredArgsConstructor
public class TradeValidationService {

    private final PositionRepository positionRepository;

    public record ValidationResult(boolean valid, String rejectionReason) {
        public static ValidationResult ok() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult rejected(String reason) {
            return new ValidationResult(false, reason);
        }
    }

    public ValidationResult validate(Trade trade, Portfolio portfolio) {
        if (trade.getQuantity() == null || trade.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            return ValidationResult.rejected("Quantity must be greater than zero");
        }

        if (trade.getPrice() == null || trade.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return ValidationResult.rejected("Price must be greater than zero");
        }

        BigDecimal notional = trade.getQuantity().multiply(trade.getPrice());

        if (trade.getTradeType() == TradeType.BUY) {
            if (portfolio.getCashBalance().compareTo(notional) < 0) {
                return ValidationResult.rejected("Insufficient funds: required " + notional
                        + " but available " + portfolio.getCashBalance());
            }
        } else if (trade.getTradeType() == TradeType.SELL) {
            Optional<Position> position = positionRepository.findByPortfolioIdAndTicker(
                    trade.getPortfolioId(), trade.getTicker());
            BigDecimal held = position.map(Position::getQuantity).orElse(BigDecimal.ZERO);
            if (held.compareTo(trade.getQuantity()) < 0) {
                return ValidationResult.rejected("Insufficient position: holding " + held
                        + " units of " + trade.getTicker() + " but attempting to sell " + trade.getQuantity());
            }
        }

        return ValidationResult.ok();
    }
}
