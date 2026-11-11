package org.example;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import org.example.menu.MenuItem;
import org.example.menu.decorator.ExtraCheese;
import org.example.menu.decorator.ExtraSauce;
import org.example.menu.factory.BurgerFactory;
import org.example.menu.factory.PizzaFactory;
import org.example.order.Order;
import org.example.order.OrderBuilder;
import org.example.order.OrderManager;
import org.example.order.OrderStatus;
import org.example.order.observer.CustomerNotifier;
import org.example.order.observer.KitchenDisplay;
import org.example.order.observer.OrderObserver;
import org.example.pricing.PriceCalculator;
import org.example.pricing.strategy.PercentageDiscount;
import org.junit.jupiter.api.Test;

class AppTest extends OrderManagerTestSupport {
    @Test
    void allSixPatternsWorkTogetherInOneOrder() {
        MenuItem pizza = new ExtraSauce(new ExtraCheese(new PizzaFactory().createItem()));
        MenuItem burger = new BurgerFactory().createItem();
        Order order = new OrderBuilder()
                .setTable(5)
                .addItem(pizza)
                .addItem(burger)
                .setDiscount(new PercentageDiscount(10))
                .build();

        PriceCalculator calculator = new PriceCalculator();
        assertEquals(new BigDecimal("3800.00"), calculator.calculateSubtotal(order));
        assertEquals(new BigDecimal("3420.00"), calculator.calculateTotal(order));

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8);
        KitchenDisplay kitchen = new KitchenDisplay(output);
        CustomerNotifier customer = new CustomerNotifier(output);
        List<OrderStatus> events = new ArrayList<>();
        OrderObserver recorder = updated -> events.add(updated.getStatus());
        OrderManager manager = OrderManager.getInstance();
        assertSame(manager, OrderManager.getInstance());
        manager.subscribe(kitchen);
        manager.subscribe(customer);
        manager.subscribe(recorder);
        try {
            manager.registerOrder(order);
            assertTrue(order.getId() > 0);
            assertSame(order, OrderManager.getInstance().getOrder(order.getId()));
            manager.updateStatus(order.getId(), OrderStatus.COOKING);
            manager.updateStatus(order.getId(), OrderStatus.READY);
            manager.updateStatus(order.getId(), OrderStatus.DELIVERED);
        } finally {
            manager.unsubscribe(kitchen);
            manager.unsubscribe(customer);
            manager.unsubscribe(recorder);
        }

        assertEquals(List.of(OrderStatus.NEW, OrderStatus.COOKING,
                OrderStatus.READY, OrderStatus.DELIVERED), events);
        String text = bytes.toString(StandardCharsets.UTF_8);
        for (OrderStatus status : OrderStatus.values()) {
            assertTrue(text.contains("[Kitchen] Order #" + order.getId() + ", table 5: " + status));
            assertTrue(text.contains("[Customer] Table 5, your order #" + order.getId() + " is " + status));
        }
        assertTrue(text.contains("extra cheese, extra sauce"));
        assertTrue(text.contains("Burger with beef and lettuce"));
    }

    @Test
    void builderValidatesInputAndKeepsBuiltOrderIndependent() {
        assertThrows(IllegalStateException.class, () -> new OrderBuilder().build());
        assertThrows(IllegalStateException.class, () -> new OrderBuilder().setTable(1).build());
        assertThrows(IllegalArgumentException.class, () -> new OrderBuilder().setTable(0));
        assertThrows(NullPointerException.class, () -> new OrderBuilder().addItem(null));
        assertThrows(NullPointerException.class, () -> new OrderBuilder().setDiscount(null));

        OrderBuilder builder = new OrderBuilder().setTable(1)
                .addItem(new PizzaFactory().createItem());
        Order first = builder.build();
        builder.addItem(new BurgerFactory().createItem()).setDiscount(new PercentageDiscount(10));
        Order second = builder.build();

        assertEquals(1, first.getItems().size());
        assertEquals(2, second.getItems().size());
        assertThrows(UnsupportedOperationException.class, () -> first.getItems().clear());
        assertEquals(new BigDecimal("2000.00"), new PriceCalculator().calculateTotal(first));
        assertEquals(new BigDecimal("3150.00"), new PriceCalculator().calculateTotal(second));
    }

    @Test
    void strategiesChangeTheTotalAndRoundMoneyToTwoPlaces() {
        OrderBuilder builder = new OrderBuilder().setTable(1)
                .addItem(new PizzaFactory().createItem());
        PriceCalculator calculator = new PriceCalculator();
        assertEquals(new BigDecimal("2000.00"), calculator.calculateTotal(builder.build()));
        assertEquals(new BigDecimal("1800.00"), calculator.calculateTotal(
                builder.setDiscount(new PercentageDiscount(10)).build()));
        assertEquals(new BigDecimal("0.00"), calculator.calculateTotal(
                builder.setDiscount(new PercentageDiscount(100)).build()));
        assertThrows(IllegalArgumentException.class, () -> new PercentageDiscount(-1));
        assertThrows(IllegalArgumentException.class, () -> new PercentageDiscount(101));

        MenuItem fractionalPrice = new MenuItem() {
            public String getName() { return "Test dish"; }
            public BigDecimal getPrice() { return new BigDecimal("10.05"); }
            public String getDescription() { return "Rounding example"; }
        };
        Order roundingExample = new OrderBuilder().setTable(1).addItem(fractionalPrice)
                .setDiscount(new PercentageDiscount(10)).build();
        assertEquals(new BigDecimal("9.05"), calculator.calculateTotal(roundingExample));
    }

    @Test
    void managerRejectsInvalidTransitionsAndSupportsUnsubscription() {
        OrderManager manager = OrderManager.getInstance();
        Order order = new OrderBuilder().setTable(2).addItem(new BurgerFactory().createItem()).build();
        List<OrderStatus> events = new ArrayList<>();
        OrderObserver observer = updated -> events.add(updated.getStatus());
        manager.subscribe(observer);
        manager.subscribe(observer);
        try {
            manager.registerOrder(order);
            assertThrows(IllegalStateException.class, () -> manager.registerOrder(order));
            assertThrows(IllegalArgumentException.class,
                    () -> manager.updateStatus(order.getId(), OrderStatus.READY));
            assertEquals(OrderStatus.NEW, order.getStatus());
            assertEquals(List.of(OrderStatus.NEW), events);
            manager.updateStatus(order.getId(), OrderStatus.COOKING);
            manager.unsubscribe(observer);
            manager.updateStatus(order.getId(), OrderStatus.READY);
            manager.updateStatus(order.getId(), OrderStatus.DELIVERED);
            assertEquals(List.of(OrderStatus.NEW, OrderStatus.COOKING), events);
            assertThrows(IllegalArgumentException.class,
                    () -> manager.updateStatus(order.getId(), OrderStatus.NEW));
            assertThrows(IllegalArgumentException.class, () -> manager.getOrder(-1));
        } finally {
            manager.unsubscribe(observer);
        }
    }

    @Test
    void consoleCreatesAnOrderAndRecoversFromInvalidInput() {
        String input = "wrong\n1\n0\n5\n3\n1\n3\n1\n2\n0\n0\n3\n2\n2\n0\n";
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (Scanner scanner = new Scanner(input);
                PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            new App(scanner, output).run();
        }
        String text = bytes.toString(StandardCharsets.UTF_8);
        assertTrue(text.contains("Enter a whole number from 0 to 3."));
        assertTrue(text.contains("Enter a whole number from 0 to 2."));
        assertTrue(text.contains("Enter a whole number from 1 to 2."));
        assertTrue(text.contains("Enter a whole number from 1 to 2147483647."));
        assertTrue(text.contains("Pizza + extra cheese + extra sauce - 2300.00 KZT"));
        assertTrue(text.contains("Total: 2070.00 KZT"));
        assertTrue(text.contains("[Kitchen]"));
        assertTrue(text.contains("[Customer]"));
        assertTrue(text.endsWith("Goodbye." + System.lineSeparator()));
    }

    @Test
    void consoleCancelsEmptyOrdersAndHandlesClosedInput() {
        int countBefore = OrderManager.getInstance().getOrders().size();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (Scanner scanner = new Scanner("1\n1\n0\n");
                PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            new App(scanner, output).run();
        }
        String text = bytes.toString(StandardCharsets.UTF_8);
        assertTrue(text.contains("Empty order cancelled."));
        assertTrue(text.contains("Input closed."));
        assertEquals(countBefore, OrderManager.getInstance().getOrders().size());
    }
}
