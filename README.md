# Final Project - Restaurant Ordering System

**Group:** SE-2539

**Team members:**

- Aldiyar Kairollin
- Ilya Koval
- Aidar Murat

A restaurant ordering application built with Java 25 and Gradle, with both a
console interface and a JavaFX graphical interface: order creation,
dish toppings, discounts, and order status notifications.
Orders are saved to `orders.json` after creation and each status change,
and loaded automatically at startup. With the Gradle run command below,
the file is located at `app/orders.json`. Order IDs, tables, dishes with
toppings, prices, discounts, and statuses are preserved between runs.

## Running the application

From the project root:

Console version:

```bash
./gradlew --console=plain :app:run
```

JavaFX GUI:

```bash
./gradlew :app:runGui
```

Both versions share the same `orders.json`.


## Building and testing

```bash
./gradlew :app:test :app:build
```

HTML JUnit test report: `app/build/reports/tests/test/index.html`.

HTML JaCoCo coverage report: `app/build/reports/jacoco/test/html/index.html`.


## Design patterns and responsibilities

Group: **SE-2539**.

| Student | Patterns | Files |
| --- | --- | --- |
| Aldiyar Kairollin | Factory Method, Decorator | `menu/`, `menu/factory/`, `menu/decorator/` |
| Ilya Koval | Singleton, Observer | `order/OrderManager.java`, `order/observer/`; also the order model and statuses |
| Aidar Murat | Builder, Strategy | `order/OrderBuilder.java`, `pricing/`, `pricing/strategy/` |

All paths in the table are relative to `app/src/main/java/org/example/`.
`App.java` (console) and `ui/RestaurantFxApp.java` (JavaFX) integrate the work of all three students.
`order/JsonOrderStorage.java` handles saving and loading orders as JSON.
The `ui/` package is excluded from the JaCoCo coverage check.

## UML diagrams

### Class diagram

Main classes and representative implementations of all six design patterns.

![Class diagram](docs/uml1.png)

### Activity diagram

Order creation, price calculation, and status notifications.

![Order workflow activity diagram](docs/uml2.png)

## Features

- Dishes: Pizza (2000 KZT), Burger (1500 KZT).
- Toppings: extra cheese (+200), extra sauce (+100 KZT).
  Toppings can be combined and repeated.
- Discounts: none or 10%.
- An order can contain multiple dishes. Each order is associated with a table number.
- Statuses: `NEW -> COOKING -> READY -> DELIVERED`.
- The kitchen and customer receive notifications when an order is registered and whenever its status changes (printed to the console, or shown in the GUI notifications box).
- JavaFX GUI: choose a table, dish, toppings and discount, build a cart, place the order, and advance the status of the selected order.
- All amounts use `BigDecimal` and are rounded to two decimal places.

## Screenshots

The screenshots show the console version.

### Main menu

Create orders, view existing orders, advance their status, or exit.

![Main menu](docs/screenshots/1.png)

### Order creation

Pizza with extra cheese and sauce and a 10% discount totals 2070.00 KZT. The kitchen and customer receive NEW order notifications.

![Order creation with toppings and discount](docs/screenshots/2.png)

### Order details

View the order ID, table, status, items, subtotal, discount, and total.

![Order details](docs/screenshots/3.png)

### Status update

Advance the order to COOKING and notify both the kitchen and customer.

![Order status update and notifications](docs/screenshots/4.png)

## Demo scenario

1. Select `1. Create order` and enter table `5`.
2. Choose Pizza and add extra cheese and extra sauce.
3. Enter `0` to finish toppings, then `0` again to finish selecting dishes.
4. Select discount `2. 10%`.
5. Check the kitchen and customer notifications, subtotal of `2300.00 KZT`, and total of `2070.00 KZT`.
6. Select `2. View orders` and note the order ID.
7. Select `3. Advance order status` three times, entering that ID each time:
   `COOKING`, then `READY`, then `DELIVERED`.
8. Select `0. Exit`.
