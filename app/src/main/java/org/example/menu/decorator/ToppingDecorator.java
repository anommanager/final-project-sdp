package org.example.menu.decorator;

import java.math.BigDecimal;
import java.util.Objects;
import org.example.menu.MenuItem;

public abstract class ToppingDecorator implements MenuItem {
    private final MenuItem item;
    private final String topping;
    private final BigDecimal extraPrice;

    protected ToppingDecorator(MenuItem item, String topping, BigDecimal extraPrice) {
        this.item = Objects.requireNonNull(item, "Dish is required");
        this.topping = topping;
        this.extraPrice = extraPrice;
    }

    @Override
    public String getName() {
        return item.getName() + " + " + topping;
    }

    @Override
    public BigDecimal getPrice() {
        return item.getPrice().add(extraPrice);
    }

    @Override
    public String getDescription() {
        return item.getDescription() + ", " + topping;
    }
}
