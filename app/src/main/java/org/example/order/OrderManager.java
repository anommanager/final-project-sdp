package org.example.order;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.example.order.observer.OrderObserver;
import org.example.order.observer.OrderSubject;

public final class OrderManager implements OrderSubject {
    private static final OrderManager INSTANCE = new OrderManager(Path.of("orders.json"));

    private final JsonOrderStorage storage;
    private final Map<Integer, Order> orders = new LinkedHashMap<>();
    private final List<OrderObserver> observers = new ArrayList<>();
    private int nextId = 1;

    private OrderManager(Path path) {
        storage = new JsonOrderStorage(path);
        for (Order order : storage.load()) {
            if (orders.putIfAbsent(order.getId(), order) != null) {
                throw new IllegalArgumentException("Duplicate order ID: " + order.getId());
            }
            nextId = Math.max(nextId, order.getId() + 1);
        }
    }

    public static OrderManager getInstance() {
        return INSTANCE;
    }

    public void registerOrder(Order order) {
        Objects.requireNonNull(order, "Order is required");
        order.assignId(nextId);
        nextId++;
        orders.put(order.getId(), order);
        storage.save(getOrders());
        notifyObservers(order);
    }

    public Order getOrder(int id) {
        Order order = orders.get(id);
        if (order == null) {
            throw new IllegalArgumentException("Order #" + id + " was not found");
        }
        return order;
    }

    public List<Order> getOrders() {
        return List.copyOf(orders.values());
    }

    public void updateStatus(int id, OrderStatus status) {
        Order order = getOrder(id);
        order.changeStatus(status);
        storage.save(getOrders());
        notifyObservers(order);
    }

    @Override
    public void subscribe(OrderObserver observer) {
        Objects.requireNonNull(observer, "Observer is required");
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    @Override
    public void unsubscribe(OrderObserver observer) {
        observers.remove(observer);
    }

    private void notifyObservers(Order order) {
        for (OrderObserver observer : List.copyOf(observers)) {
            observer.update(order);
        }
    }
}
