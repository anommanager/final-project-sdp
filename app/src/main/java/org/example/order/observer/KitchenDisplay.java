package org.example.order.observer;

import java.io.PrintStream;
import org.example.order.Order;
import org.example.order.OrderStatus;

public class KitchenDisplay implements OrderObserver {
    private final PrintStream output;

    public KitchenDisplay(PrintStream output) {
        this.output = output;
    }

    @Override
    public void update(Order order) {
        output.printf("[Kitchen] Order #%d, table %d: %s%n",
                order.getId(), order.getTable(), order.getStatus());
        if (order.getStatus() == OrderStatus.NEW) {
            order.getItems().forEach(item -> output.println("  " + item.getDescription()));
        }
    }
}
