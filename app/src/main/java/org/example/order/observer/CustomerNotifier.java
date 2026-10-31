package org.example.order.observer;

import java.io.PrintStream;
import org.example.order.Order;

public class CustomerNotifier implements OrderObserver {
    private final PrintStream output;

    public CustomerNotifier(PrintStream output) {
        this.output = output;
    }

    @Override
    public void update(Order order) {
        output.printf("[Customer] Table %d, your order #%d is %s%n",
                order.getTable(), order.getId(), order.getStatus());
    }
}
