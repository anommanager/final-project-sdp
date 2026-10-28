package org.example.order.observer;

import org.example.order.Order;

public interface OrderObserver {
    void update(Order order);
}
