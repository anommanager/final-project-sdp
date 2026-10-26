package org.example.menu;

import java.math.BigDecimal;

public class Burger implements MenuItem {
    @Override
    public String getName() {
        return "Burger";
    }

    @Override
    public BigDecimal getPrice() {
        return new BigDecimal("1500.00");
    }

    @Override
    public String getDescription() {
        return "Burger with beef and lettuce";
    }
}
