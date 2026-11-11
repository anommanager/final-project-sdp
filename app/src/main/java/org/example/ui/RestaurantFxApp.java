package org.example.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
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
import org.example.order.observer.OrderObserver;
import org.example.pricing.PriceCalculator;
import org.example.pricing.strategy.NoDiscount;
import org.example.pricing.strategy.PercentageDiscount;

public class RestaurantFxApp extends Application {
    private final OrderManager manager = OrderManager.getInstance();
    private final PriceCalculator calculator = new PriceCalculator();
    private final MenuItemFactory[] factories = {new PizzaFactory(), new BurgerFactory()};

    private final ObservableList<MenuItem> cart = FXCollections.observableArrayList();
    private final ObservableList<String> orderRows = FXCollections.observableArrayList();
    private final List<Order> shownOrders = new ArrayList<>();
    private final ListView<String> orderList = new ListView<>(orderRows);
    private final TextArea notifications = new TextArea();
    private final Label cartTotal = new Label("Subtotal: 0.00 KZT");

    private final OrderObserver observer = order -> Platform.runLater(() -> {
        notifications.appendText(String.format("[Kitchen/Customer] Table %d, order #%d is %s%n",
                order.getTable(), order.getId(), order.getStatus()));
        refreshOrders();
    });

    @Override
    public void start(Stage stage) {
        manager.subscribe(observer);

        Spinner<Integer> table = new Spinner<>(1, 999, 1);
        table.setEditable(true);

        ComboBox<MenuItemFactory> dish = new ComboBox<>(FXCollections.observableArrayList(factories));
        Function<MenuItemFactory, String> label = f -> {
            MenuItem item = f.createItem();
            return item.getName() + " - " + item.getPrice() + " KZT";
        };
        dish.setCellFactory(v -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(MenuItemFactory f, boolean empty) {
                super.updateItem(f, empty);
                setText(empty || f == null ? null : label.apply(f));
            }
        });
        dish.setButtonCell(dish.getCellFactory().call(null));
        dish.getSelectionModel().selectFirst();

        CheckBox cheese = new CheckBox("Extra cheese +200.00");
        CheckBox sauce = new CheckBox("Extra sauce +100.00");
        CheckBox discount = new CheckBox("10% discount");

        ListView<MenuItem> cartList = new ListView<>(cart);
        cartList.setCellFactory(v -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(MenuItem item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName() + " - " + item.getPrice() + " KZT");
            }
        });
        cartList.setPrefHeight(150);

        Button add = new Button("Add dish");
        add.setOnAction(e -> {
            MenuItem item = dish.getValue().createItem();
            if (cheese.isSelected()) {
                item = new ExtraCheese(item);
            }
            if (sauce.isSelected()) {
                item = new ExtraSauce(item);
            }
            cart.add(item);
            updateCartTotal(discount.isSelected());
        });
        Button remove = new Button("Remove selected");
        remove.setOnAction(e -> {
            cart.remove(cartList.getSelectionModel().getSelectedItem());
            updateCartTotal(discount.isSelected());
        });
        discount.setOnAction(e -> updateCartTotal(discount.isSelected()));

        Button place = new Button("Place order");
        place.setDefaultButton(true);
        place.setOnAction(e -> {
            try {
                table.commitValue();
                Order order = new OrderBuilder().setTable(table.getValue())
                        .setDiscount(discount.isSelected() ? new PercentageDiscount(10) : new NoDiscount())
                        .addAllItems(cart).build();
                manager.registerOrder(order);
                cart.clear();
                updateCartTotal(discount.isSelected());
            } catch (RuntimeException ex) {
                new Alert(Alert.AlertType.WARNING, ex.getMessage()).showAndWait();
            }
        });

        VBox left = new VBox(8, new Label("New order"), new HBox(8, new Label("Table:"), table),
                new Label("Dish:"), dish, cheese, sauce, add, cartList, remove, discount, cartTotal, place);
        left.setPadding(new Insets(12));
        left.setPrefWidth(320);

        Button advance = new Button("Advance status");
        advance.setOnAction(e -> {
            int index = orderList.getSelectionModel().getSelectedIndex();
            if (index < 0) {
                return;
            }
            Order order = shownOrders.get(index);
            if (order.getStatus() == OrderStatus.DELIVERED) {
                new Alert(Alert.AlertType.INFORMATION, "This order is already delivered.").showAndWait();
                return;
            }
            manager.updateStatus(order.getId(),
                    OrderStatus.values()[order.getStatus().ordinal() + 1]);
            orderList.getSelectionModel().select(index);
        });

        notifications.setEditable(false);
        notifications.setPrefRowCount(6);
        VBox.setVgrow(orderList, Priority.ALWAYS);
        VBox right = new VBox(8, new Label("Orders"), orderList, advance,
                new Label("Notifications"), notifications);
        right.setPadding(new Insets(12));
        HBox.setHgrow(right, Priority.ALWAYS);

        stage.setTitle("Restaurant Ordering System");
        stage.setScene(new Scene(new HBox(left, right), 820, 560));
        stage.show();
        refreshOrders();
    }

    @Override
    public void stop() {
        manager.unsubscribe(observer);
    }

    private void updateCartTotal(boolean tenPercent) {
        java.math.BigDecimal sum = cart.stream().map(MenuItem::getPrice)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        java.math.BigDecimal total = (tenPercent ? new PercentageDiscount(10) : new NoDiscount())
                .apply(sum).setScale(2, java.math.RoundingMode.HALF_UP);
        cartTotal.setText("Total: " + total + " KZT");
    }

    private void refreshOrders() {
        int selected = orderList.getSelectionModel().getSelectedIndex();
        shownOrders.clear();
        shownOrders.addAll(manager.getOrders());
        orderRows.clear();
        for (Order order : shownOrders) {
            orderRows.add(String.format("#%d | Table %d | %s | %d item(s) | %s KZT (%s)",
                    order.getId(), order.getTable(), order.getStatus(), order.getItems().size(),
                    calculator.calculateTotal(order), order.getDiscount().getDescription()));
        }
        orderList.getSelectionModel().select(selected);
    }
}
