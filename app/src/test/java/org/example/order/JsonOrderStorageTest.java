package org.example.order;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParseException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.example.menu.MenuItem;
import org.example.menu.decorator.ExtraCheese;
import org.example.menu.decorator.ExtraSauce;
import org.example.menu.factory.PizzaFactory;
import org.example.pricing.PriceCalculator;
import org.example.pricing.strategy.NoDiscount;
import org.example.pricing.strategy.PercentageDiscount;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JsonOrderStorageTest {
    @TempDir
    Path directory;

    @Test
    void missingFileStartsWithNoOrders() {
        Path path = directory.resolve("orders.json");

        assertTrue(openManager(path).getOrders().isEmpty());
        assertFalse(Files.exists(path));
    }

    @Test
    void restoresOrdersAndContinuesIdsAfterRestart() throws Exception {
        Path path = directory.resolve("orders.json");
        OrderManager manager = openManager(path);
        MenuItem pizza = new ExtraSauce(new ExtraCheese(new ExtraCheese(new PizzaFactory().createItem())));
        Order first = new OrderBuilder().setTable(5).addItem(pizza)
                .addItem(new PizzaFactory().createItem())
                .setDiscount(new PercentageDiscount(10)).build();
        manager.registerOrder(first);
        manager.updateStatus(first.getId(), OrderStatus.COOKING);
        manager.updateStatus(first.getId(), OrderStatus.READY);
        manager.registerOrder(new OrderBuilder().setTable(8)
                .addItem(new PizzaFactory().createItem()).build());

        OrderManager restarted = openManager(path);
        Order restored = restarted.getOrder(first.getId());

        assertEquals(2, restarted.getOrders().size());
        assertEquals(5, restored.getTable());
        assertEquals(OrderStatus.READY, restored.getStatus());
        assertEquals(2, restored.getItems().size());
        assertEquals(pizza.getName(), restored.getItems().getFirst().getName());
        assertEquals(pizza.getDescription(), restored.getItems().getFirst().getDescription());
        assertEquals(pizza.getPrice(), restored.getItems().getFirst().getPrice());
        assertEquals("10% discount", restored.getDiscount().getDescription());
        assertEquals(new BigDecimal("4050.00"), new PriceCalculator().calculateTotal(restored));
        assertInstanceOf(NoDiscount.class, restarted.getOrder(2).getDiscount());
        assertEquals(OrderStatus.NEW, restarted.getOrder(2).getStatus());

        restarted.updateStatus(restored.getId(), OrderStatus.DELIVERED);
        Order third = new OrderBuilder().setTable(3).addItem(new PizzaFactory().createItem()).build();
        restarted.registerOrder(third);

        assertEquals(3, third.getId());
        OrderManager nextRestart = openManager(path);
        assertEquals(OrderStatus.DELIVERED, nextRestart.getOrder(first.getId()).getStatus());
        assertEquals(3, nextRestart.getOrders().size());
        assertTrue(Files.readString(path).contains("\"status\": \"DELIVERED\""));
        assertFalse(Files.exists(directory.resolve("orders.json.tmp")));
    }

    @Test
    void preservesZeroPercentDiscountAndUsesTheHighestSavedId() {
        Path path = directory.resolve("orders.json");
        Order order = new OrderBuilder().setTable(1).addItem(new PizzaFactory().createItem())
                .setDiscount(new PercentageDiscount(0)).build();
        order.assignId(12);
        new JsonOrderStorage(path).save(List.of(order));

        OrderManager restarted = openManager(path);
        Order next = new OrderBuilder().setTable(2).addItem(new PizzaFactory().createItem()).build();
        restarted.registerOrder(next);

        assertEquals("0% discount", restarted.getOrder(12).getDiscount().getDescription());
        assertEquals(13, next.getId());
    }

    @Test
    void savesAndLoadsAnEmptyList() throws Exception {
        Path path = directory.resolve("orders.json");
        JsonOrderStorage storage = new JsonOrderStorage(path);

        storage.save(List.of());

        assertEquals("[]", Files.readString(path));
        assertTrue(storage.load().isEmpty());
    }

    @Test
    void invalidJsonIsNotOverwritten() throws Exception {
        Path path = directory.resolve("orders.json");
        Files.writeString(path, "invalid json");

        assertThrows(JsonParseException.class, () -> openManager(path));
        assertEquals("invalid json", Files.readString(path));

        Files.writeString(path, "null");
        assertThrows(NullPointerException.class, () -> openManager(path));
    }

    @Test
    void rejectsInvalidIdsMissingStatusesAndDuplicateIds() throws Exception {
        Path path = directory.resolve("orders.json");
        Files.writeString(path, "[{\"id\":0,\"status\":\"NEW\"}]");
        assertThrows(IllegalArgumentException.class, () -> openManager(path));

        Files.writeString(path, "[{\"id\":1}]");
        assertThrows(NullPointerException.class, () -> openManager(path));

        Order order = new OrderBuilder().setTable(1).addItem(new PizzaFactory().createItem()).build();
        order.assignId(1);
        new JsonOrderStorage(path).save(List.of(order, order));
        assertThrows(IllegalArgumentException.class, () -> openManager(path));
    }

    @Test
    void reportsFileReadAndWriteErrors() {
        assertThrows(UncheckedIOException.class, () -> new JsonOrderStorage(directory).load());
        Path missingParent = directory.resolve("missing").resolve("orders.json");
        assertThrows(UncheckedIOException.class,
                () -> new JsonOrderStorage(missingParent).save(List.of()));
    }

    private OrderManager openManager(Path path) {
        try {
            var constructor = OrderManager.class.getDeclaredConstructor(Path.class);
            constructor.setAccessible(true);
            return constructor.newInstance(path);
        } catch (InvocationTargetException exception) {
            throw (RuntimeException) exception.getCause();
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
