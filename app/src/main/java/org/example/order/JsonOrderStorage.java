package org.example.order;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.example.menu.MenuItem;
import org.example.pricing.strategy.NoDiscount;
import org.example.pricing.strategy.PercentageDiscount;

final class JsonOrderStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path path;

    JsonOrderStorage(Path path) {
        this.path = path;
    }

    List<Order> load() {
        if (Files.notExists(path)) {
            return List.of();
        }
        try {
            SavedOrder[] saved = Objects.requireNonNull(
                    GSON.fromJson(Files.readString(path), SavedOrder[].class), "Orders JSON is empty");
            return Arrays.stream(saved).map(SavedOrder::toOrder).toList();
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot read orders from " + path, exception);
        }
    }

    void save(List<Order> orders) {
        List<SavedOrder> saved = orders.stream().map(SavedOrder::from).toList();
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            Files.writeString(temporary, GSON.toJson(saved));
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot save orders to " + path, exception);
        }
    }

    private record SavedOrder(int id, int table, List<SavedItem> items,
                              Integer discountPercent, OrderStatus status) {
        static SavedOrder from(Order order) {
            Integer percent = order.getDiscount() instanceof PercentageDiscount discount
                    ? discount.getPercent() : null;
            return new SavedOrder(order.getId(), order.getTable(),
                    order.getItems().stream().map(SavedItem::from).toList(),
                    percent, order.getStatus());
        }

        Order toOrder() {
            if (id <= 0) {
                throw new IllegalArgumentException("Order ID must be positive");
            }
            Objects.requireNonNull(status, "Order status is required");
            Order order = new OrderBuilder().setTable(table).addAllItems(items)
                    .setDiscount(discountPercent == null
                            ? new NoDiscount() : new PercentageDiscount(discountPercent)).build();
            order.assignId(id);
            while (order.getStatus() != status) {
                order.changeStatus(OrderStatus.values()[order.getStatus().ordinal() + 1]);
            }
            return order;
        }
    }

    private record SavedItem(String name, BigDecimal price, String description) implements MenuItem {
        static SavedItem from(MenuItem item) {
            return new SavedItem(item.getName(), item.getPrice(), item.getDescription());
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public BigDecimal getPrice() {
            return price;
        }

        @Override
        public String getDescription() {
            return description;
        }
    }
}
