package org.example;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import org.example.menu.factory.BurgerFactory;
import org.example.order.Order;
import org.example.order.OrderBuilder;
import org.example.order.OrderStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AppConsoleTest extends OrderManagerTestSupport {
    @Test
    void viewingAndAdvancingWithoutOrdersShowsHelpfulMessages() {
        String text = runApp("2", "3", "0");

        assertEquals(2, text.lines().filter(line -> line.contains("No orders yet.")).count());
        assertTrue(manager.getOrders().isEmpty());
        assertTrue(text.endsWith("Goodbye." + System.lineSeparator()));
    }

    @Test
    void createsAndListsMultipleDishesWithoutDiscount() {
        String text = runApp("1", "7", "2", "0", "1", "0", "0", "1", "2", "0");

        assertEquals(1, manager.getOrders().size());
        Order order = manager.getOrders().getFirst();
        assertEquals(7, order.getTable());
        assertEquals(2, order.getItems().size());
        assertEquals("Burger", order.getItems().get(0).getName());
        assertEquals("Pizza", order.getItems().get(1).getName());
        assertTrue(text.contains("Order #" + order.getId() + " | Table 7 | NEW"));
        assertTrue(text.contains("Burger - 1500.00 KZT"));
        assertTrue(text.contains("Pizza - 2000.00 KZT"));
        assertTrue(text.contains("Subtotal: 3500.00 KZT"));
        assertTrue(text.contains("Discount: No discount"));
        assertEquals(2, text.lines().filter(line -> line.equals("Total: 3500.00 KZT")).count());
    }

    @Test
    void repeatedToppingsAreIncludedInTheDiscountedTotal() {
        String text = runApp("1", "1", "2", "1", "1", "2", "0", "0", "2", "0");

        Order order = manager.getOrders().getFirst();
        assertEquals("Burger + extra cheese + extra cheese + extra sauce",
                order.getItems().getFirst().getName());
        assertTrue(text.contains("Subtotal: 2000.00 KZT"));
        assertTrue(text.contains("Discount: 10% discount"));
        assertTrue(text.contains("Total: 1800.00 KZT"));
    }

    @Test
    void advancesThroughAllStatusesAndRejectsDeliveredOrders() {
        Order order = registerOrder();
        String id = Integer.toString(order.getId());

        String text = runApp("3", id, "3", id, "3", id, "3", id, "0");

        assertEquals(OrderStatus.DELIVERED, order.getStatus());
        for (OrderStatus status : new OrderStatus[] {
                OrderStatus.COOKING, OrderStatus.READY, OrderStatus.DELIVERED}) {
            assertTrue(text.contains("[Kitchen] Order #" + id + ", table 4: " + status));
            assertTrue(text.contains("[Customer] Table 4, your order #" + id + " is " + status));
        }
        assertTrue(text.contains("This order is already delivered."));
    }

    @Test
    void unknownOrderAndInvalidIdsDoNotStopTheApplication() {
        Order order = registerOrder();

        String text = runApp("3", "wrong", "-1", "0", "2147483648", "2147483647", "2", "0");

        assertTrue(text.contains("Order #2147483647 was not found"));
        assertEquals(4, text.lines().filter(line ->
                line.contains("Enter a whole number from 1 to 2147483647.")).count());
        assertTrue(text.contains("Order #" + order.getId() + " | Table 4 | NEW"));
        assertEquals(OrderStatus.NEW, order.getStatus());
        assertTrue(text.endsWith("Goodbye." + System.lineSeparator()));
    }

    @Test
    void rejectsOutOfRangeAndOverflowingMenuInputAndTrimsWhitespace() {
        String text = runApp("-1", "4", "2147483648", "", " 0 ");

        assertEquals(4, text.lines().filter(line ->
                line.contains("Enter a whole number from 0 to 3.")).count());
        assertFalse(text.contains("Input closed."));
        assertTrue(manager.getOrders().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "1\n", "1\n2\n", "1\n2\n1\n", "1\n2\n1\n0\n0\n"})
    void closedInputCancelsUnfinishedOrders(String input) {
        String text = runInput(input);

        assertTrue(text.contains("Input closed."));
        assertTrue(text.endsWith("Goodbye." + System.lineSeparator()));
        assertTrue(manager.getOrders().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0\n", ""})
    void removesApplicationObserversAfterExitAndClosedInput(String input) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (Scanner scanner = new Scanner(input);
                PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            new App(scanner, output).run();
            String textBefore = bytes.toString(StandardCharsets.UTF_8);

            Order order = registerOrder();
            manager.updateStatus(order.getId(), OrderStatus.COOKING);

            assertEquals(textBefore, bytes.toString(StandardCharsets.UTF_8));
        }
    }

    @Test
    void closingInputWhileChoosingAnOrderLeavesItsStatusUnchanged() {
        Order order = registerOrder();

        String text = runInput("3\n");

        assertTrue(text.contains("Order ID: "));
        assertTrue(text.contains("Input closed."));
        assertEquals(OrderStatus.NEW, order.getStatus());
    }

    @Test
    @ResourceLock("system-streams")
    void mainUsesStandardInputAndOutput() {
        InputStream originalInput = System.in;
        PrintStream originalOutput = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            System.setIn(new ByteArrayInputStream("0\n".getBytes(StandardCharsets.UTF_8)));
            System.setOut(output);

            App.main(new String[0]);
        } finally {
            System.setIn(originalInput);
            System.setOut(originalOutput);
        }

        String text = bytes.toString(StandardCharsets.UTF_8);
        assertTrue(text.startsWith("=== Restaurant Ordering System ==="));
        assertTrue(text.endsWith("Goodbye." + System.lineSeparator()));
        assertFalse(text.contains("Input closed."));
    }

    private Order registerOrder() {
        Order order = new OrderBuilder().setTable(4)
                .addItem(new BurgerFactory().createItem()).build();
        manager.registerOrder(order);
        return order;
    }

    private String runApp(String... lines) {
        return runInput(String.join("\n", lines) + "\n");
    }

    private String runInput(String input) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (Scanner scanner = new Scanner(input);
                PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            new App(scanner, output).run();
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }
}
