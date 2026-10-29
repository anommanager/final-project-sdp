package org.example.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.example.menu.MenuItem;
import org.example.order.Order;

public class PriceCalculator {
    public BigDecimal calculateSubtotal(Order order) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (MenuItem item : order.getItems()) {
            subtotal = subtotal.add(item.getPrice());
        }
        return subtotal.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateTotal(Order order) {
        return order.getDiscount().apply(calculateSubtotal(order))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
