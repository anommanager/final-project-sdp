package org.example.menu;

import java.math.BigDecimal;

public class Pizza implements MenuItem {
    @Override
    public String getName() {
        return "Pizza";
    }

    @Override
    public BigDecimal getPrice() {
        return new BigDecimal("2000.00");
    }

    @Override
    public String getDescription() {
        return "Pizza with tomato and mozzarella";
    }
}
