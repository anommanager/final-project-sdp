package org.example;

import java.io.PrintStream;
import java.util.NoSuchElementException;
import java.util.Scanner;
import org.example.menu.MenuItem;
import org.example.menu.decorator.ExtraCheese;
import org.example.menu.decorator.ExtraSauce;
import org.example.menu.factory.BurgerFactory;
import org.example.menu.factory.MenuItemFactory;
import org.example.menu.factory.PizzaFactory;
import org.example.order.Order;
import org.example.order.OrderBuilder;
import org.example.order.OrderManager;
import org.example.order.OrderStatus;
import org.example.order.observer.CustomerNotifier;
import org.example.order.observer.KitchenDisplay;
import org.example.pricing.PriceCalculator;
import org.example.pricing.strategy.DiscountStrategy;
import org.example.pricing.strategy.NoDiscount;
import org.example.pricing.strategy.PercentageDiscount;

public class App {
    private final Scanner input;
    private final PrintStream output;
    private final OrderManager manager = OrderManager.getInstance();
    private final PriceCalculator calculator = new PriceCalculator();
    private final MenuItemFactory[] factories = {
        new PizzaFactory(), new BurgerFactory()
    };

    App(Scanner input, PrintStream output) {
        this.input = input;
        this.output = output;
    }

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            new App(scanner, System.out).run();
        }
    }

    void run() {
        KitchenDisplay kitchen = new KitchenDisplay(output);
        CustomerNotifier customer = new CustomerNotifier(output);
        manager.subscribe(kitchen);
        manager.subscribe(customer);
        output.println("Restaurant Ordering System");
        try {
            boolean running = true;
            while (running) {
                output.println("\n1. Create order\n2. View orders\n3. Advance order status\n0. Exit");
                switch (readInt("Choose: ", 0, 3)) {
                    case 1 -> createOrder();
                    case 2 -> showOrders();
                    case 3 -> advanceStatus();
                    case 0 -> running = false;
                    default -> throw new IllegalStateException("Unexpected menu option");
                }
            }
        } catch (NoSuchElementException exception) {
            output.println("\nInput closed.");
        } finally {
            manager.unsubscribe(kitchen);
            manager.unsubscribe(customer);
        }
        output.println("Goodbye.");
    }

    private void createOrder() {
        OrderBuilder builder = new OrderBuilder()
                .setTable(readInt("Table number: ", 1, Integer.MAX_VALUE));
        int itemCount = 0;
        while (true) {
            output.println("\nDishes:");
            for (int index = 0; index < factories.length; index++) {
                MenuItem item = factories[index].createItem();
                output.printf("%d. %s - %s KZT%n", index + 1, item.getName(), item.getPrice());
            }
            output.println("0. Finish choosing dishes");
            int choice = readInt("Dish: ", 0, factories.length);
            if (choice == 0) {
                break;
            }
            MenuItem item = addToppings(factories[choice - 1].createItem());
            builder.addItem(item);
            itemCount++;
            output.println("Added: " + item.getName());
        }
        if (itemCount == 0) {
            output.println("Empty order cancelled.");
            return;
        }
        output.println("\nDiscount:\n1. None\n2. 10%");
        DiscountStrategy discount = switch (readInt("Discount: ", 1, 2)) {
            case 1 -> new NoDiscount();
            case 2 -> new PercentageDiscount(10);
            default -> throw new IllegalStateException("Unexpected discount option");
        };
        Order order = builder.setDiscount(discount).build();
        manager.registerOrder(order);
        printOrder(order);
    }

    private MenuItem addToppings(MenuItem item) {
        while (true) {
            output.println("\nToppings:\n1. Extra cheese +200.00 KZT\n2. Extra sauce +100.00 KZT"
                    + "\n0. Finish toppings");
            int choice = readInt("Topping: ", 0, 2);
            if (choice == 0) {
                return item;
            }
            item = switch (choice) {
                case 1 -> new ExtraCheese(item);
                case 2 -> new ExtraSauce(item);
                default -> throw new IllegalStateException("Unexpected topping option");
            };
        }
    }

    private void showOrders() {
        if (manager.getOrders().isEmpty()) {
            output.println("No orders yet.");
            return;
        }
        for (Order order : manager.getOrders()) {
            printOrder(order);
        }
    }

    private void printOrder(Order order) {
        output.printf("%nOrder #%d | Table %d | %s%n",
                order.getId(), order.getTable(), order.getStatus());
        for (MenuItem item : order.getItems()) {
            output.printf("  %s - %s KZT%n", item.getName(), item.getPrice());
        }
        output.println("Subtotal: " + calculator.calculateSubtotal(order) + " KZT");
        output.println("Discount: " + order.getDiscount().getDescription());
        output.println("Total: " + calculator.calculateTotal(order) + " KZT");
    }

    private void advanceStatus() {
        if (manager.getOrders().isEmpty()) {
            output.println("No orders yet.");
            return;
        }
        int id = readInt("Order ID: ", 1, Integer.MAX_VALUE);
        try {
            Order order = manager.getOrder(id);
            if (order.getStatus() == OrderStatus.DELIVERED) {
                output.println("This order is already delivered.");
                return;
            }
            OrderStatus next = OrderStatus.values()[order.getStatus().ordinal() + 1];
            manager.updateStatus(id, next);
        } catch (IllegalArgumentException exception) {
            output.println(exception.getMessage());
        }
    }

    private int readInt(String prompt, int minimum, int maximum) {
        while (true) {
            output.print(prompt);
            if (!input.hasNextLine()) {
                throw new NoSuchElementException("Input closed");
            }
            try {
                int value = Integer.parseInt(input.nextLine().trim());
                if (value >= minimum && value <= maximum) {
                    return value;
                }
            } catch (NumberFormatException exception) {
            }
            output.printf("Enter a whole number from %d to %d.%n", minimum, maximum);
        }
    }
}
