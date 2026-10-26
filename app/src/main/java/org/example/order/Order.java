package org.example.order;

import java.util.List;
import org.example.menu.MenuItem;
import org.example.pricing.strategy.DiscountStrategy;

public class Order {
    private int id;
    private final int table;
    private final List<MenuItem> items;
    private final DiscountStrategy discount;
    private OrderStatus status = OrderStatus.NEW;

    Order(int table, List<MenuItem> items, DiscountStrategy discount) {
        this.table = table;
        this.items = List.copyOf(items);
        this.discount = discount;
    }

    public int getId() {
        return id;
    }

    public int getTable() {
        return table;
    }

    public List<MenuItem> getItems() {
        return items;
    }

    public DiscountStrategy getDiscount() {
        return discount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    void assignId(int id) {
        if (this.id != 0) {
            throw new IllegalStateException("Order is already registered");
        }
        this.id = id;
    }

    void changeStatus(OrderStatus next) {
        if (status == OrderStatus.DELIVERED || next == null
                || next.ordinal() != status.ordinal() + 1) {
            throw new IllegalArgumentException("Expected the next status after " + status);
        }
        status = next;
    }
}
