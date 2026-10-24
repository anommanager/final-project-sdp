package org.example.pricing.strategy;

import java.math.BigDecimal;

public class NoDiscount implements DiscountStrategy {
    @Override
    public BigDecimal apply(BigDecimal subtotal) {
        return subtotal;
    }

    @Override
    public String getDescription() {
        return "No discount";
    }
}
