package org.example.pricing.strategy;

import java.math.BigDecimal;

public class PercentageDiscount implements DiscountStrategy {
    private final int percent;

    public PercentageDiscount(int percent) {
        if (percent < 0 || percent > 100) {
            throw new IllegalArgumentException("Discount must be between 0 and 100");
        }
        this.percent = percent;
    }

    @Override
    public BigDecimal apply(BigDecimal subtotal) {
        return subtotal.multiply(BigDecimal.valueOf(100 - percent)).movePointLeft(2);
    }

    @Override
    public String getDescription() {
        return percent + "% discount";
    }
}
