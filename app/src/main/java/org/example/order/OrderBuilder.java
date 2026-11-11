package org.example.order;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.example.menu.MenuItem;
import org.example.pricing.strategy.DiscountStrategy;
import org.example.pricing.strategy.NoDiscount;

public class OrderBuilder {
    private int table;
    private final List<MenuItem> items = new ArrayList<>();
    private DiscountStrategy discount = new NoDiscount();

    public OrderBuilder setTable(int table) {
        if (table <= 0) {
            throw new IllegalArgumentException("Table number must be positive");
        }
        this.table = table;
        return this;
    }

    public OrderBuilder addItem(MenuItem item) {
        items.add(Objects.requireNonNull(item, "Dish is required"));
        return this;
    }

    public OrderBuilder addAllItems(Iterable<? extends MenuItem> newItems) {
        newItems.forEach(this::addItem);
        return this;
    }

    public OrderBuilder setDiscount(DiscountStrategy discount) {
        this.discount = Objects.requireNonNull(discount, "Discount strategy is required");
        return this;
    }

    public Order build() {
        if (table <= 0) {
            throw new IllegalStateException("Select a table before building an order");
        }
        if (items.isEmpty()) {
            throw new IllegalStateException("An order must contain at least one dish");
        }
        return new Order(table, items, discount);
    }
}
