package org.example;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import org.example.order.OrderManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.parallel.ResourceLock;

@ResourceLock("order-manager")
public abstract class OrderManagerTestSupport {
    protected final OrderManager manager = OrderManager.getInstance();

    @BeforeEach
    protected final void resetManagerBeforeTest() throws ReflectiveOperationException {
        resetManager();
    }

    @AfterEach
    protected final void resetManagerAfterTest() throws ReflectiveOperationException {
        resetManager();
    }

    private void resetManager() throws ReflectiveOperationException {
        Field orders = OrderManager.class.getDeclaredField("orders");
        orders.setAccessible(true);
        ((Map<?, ?>) orders.get(manager)).clear();

        Field observers = OrderManager.class.getDeclaredField("observers");
        observers.setAccessible(true);
        ((Collection<?>) observers.get(manager)).clear();

        Field nextId = OrderManager.class.getDeclaredField("nextId");
        nextId.setAccessible(true);
        nextId.setInt(manager, 1);
    }
}
