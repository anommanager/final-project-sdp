package org.example.order;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.example.OrderManagerTestSupport;
import org.example.menu.factory.PizzaFactory;
import org.example.order.observer.OrderObserver;
import org.junit.jupiter.api.Test;

class OrderManagerTest extends OrderManagerTestSupport {
    @Test
    void rejectsNullOrdersAndObserversWithoutChangingTheRegistry() {
        assertThrows(NullPointerException.class, () -> manager.registerOrder(null));
        assertThrows(NullPointerException.class, () -> manager.subscribe(null));
        assertDoesNotThrow(() -> manager.unsubscribe(null));
        assertTrue(manager.getOrders().isEmpty());

        Order order = createOrder();
        manager.registerOrder(order);

        assertEquals(1, order.getId());
        assertEquals(List.of(order), manager.getOrders());
    }

    @Test
    void assignsSequentialIdsAndKeepsRegistrationOrderAfterDuplicateRejection() {
        Order first = createOrder();
        Order second = createOrder();
        Order third = createOrder();

        manager.registerOrder(first);
        manager.registerOrder(second);
        assertThrows(IllegalStateException.class, () -> manager.registerOrder(first));
        manager.registerOrder(third);

        assertEquals(1, first.getId());
        assertEquals(2, second.getId());
        assertEquals(3, third.getId());
        assertSame(first, manager.getOrder(first.getId()));
        assertSame(second, manager.getOrder(second.getId()));
        assertSame(third, manager.getOrder(third.getId()));
        assertEquals(List.of(first, second, third), manager.getOrders());
    }

    @Test
    void returnedOrderListIsAnImmutableSnapshot() {
        Order first = createOrder();
        manager.registerOrder(first);
        List<Order> snapshot = manager.getOrders();

        manager.registerOrder(createOrder());

        assertEquals(List.of(first), snapshot);
        assertEquals(2, manager.getOrders().size());
        assertThrows(UnsupportedOperationException.class, snapshot::clear);
    }

    @Test
    void invalidStatusChangesDoNotMutateOrdersOrNotifyObservers() {
        Order order = createOrder();
        List<OrderStatus> events = new ArrayList<>();
        manager.subscribe(updated -> events.add(updated.getStatus()));
        manager.registerOrder(order);

        assertThrows(IllegalArgumentException.class, () -> manager.updateStatus(order.getId(), null));
        assertThrows(IllegalArgumentException.class,
                () -> manager.updateStatus(order.getId(), OrderStatus.NEW));
        assertThrows(IllegalArgumentException.class,
                () -> manager.updateStatus(order.getId(), OrderStatus.DELIVERED));
        assertThrows(IllegalArgumentException.class,
                () -> manager.updateStatus(Integer.MAX_VALUE, OrderStatus.COOKING));

        assertEquals(OrderStatus.NEW, order.getStatus());
        assertEquals(List.of(OrderStatus.NEW), events);
    }

    @Test
    void subscribingAndUnsubscribingDuringNotificationAffectsTheNextEvent() {
        Order order = createOrder();
        List<OrderStatus> lateEvents = new ArrayList<>();
        List<OrderStatus> initialEvents = new ArrayList<>();
        OrderObserver lateObserver = updated -> lateEvents.add(updated.getStatus());
        OrderObserver initialObserver = new OrderObserver() {
            @Override
            public void update(Order updated) {
                initialEvents.add(updated.getStatus());
                manager.unsubscribe(this);
                manager.subscribe(lateObserver);
            }
        };
        manager.subscribe(initialObserver);

        manager.registerOrder(order);

        assertEquals(List.of(OrderStatus.NEW), initialEvents);
        assertTrue(lateEvents.isEmpty());

        manager.updateStatus(order.getId(), OrderStatus.COOKING);

        assertEquals(List.of(OrderStatus.NEW), initialEvents);
        assertEquals(List.of(OrderStatus.COOKING), lateEvents);
    }

    @Test
    void removingAnUnsubscribedObserverDoesNotAffectOtherSubscribers() {
        List<OrderStatus> events = new ArrayList<>();
        OrderObserver activeObserver = order -> events.add(order.getStatus());
        OrderObserver absentObserver = order -> fail("Unsubscribed observer was called");
        manager.subscribe(activeObserver);

        manager.unsubscribe(absentObserver);
        manager.registerOrder(createOrder());

        assertEquals(List.of(OrderStatus.NEW), events);
    }

    private Order createOrder() {
        return new OrderBuilder().setTable(1).addItem(new PizzaFactory().createItem()).build();
    }
}
