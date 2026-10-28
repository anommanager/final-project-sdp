package org.example.order.observer;

public interface OrderSubject {
    void subscribe(OrderObserver observer);

    void unsubscribe(OrderObserver observer);
}
