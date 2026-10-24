package org.example.pricing.strategy;

import java.math.BigDecimal;

public interface DiscountStrategy {
    BigDecimal apply(BigDecimal subtotal);

    String getDescription();
}
