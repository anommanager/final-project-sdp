# Restaurant Ordering System

A minimal console application built with Java 25 and Gradle: order creation,
dish toppings, discounts, and order status notifications.
Orders are stored in memory until the application exits.

## Running the application

From the project root:

```bash
./gradlew --console=plain :app:run
```

On Windows:

```powershell
.\gradlew.bat --console=plain :app:run
```

The Gradle Wrapper runs Gradle without a separate installation. Compilation uses
JDK 25; automatic toolchain downloads are already configured if needed.
The first run may require internet access to download Gradle, the JDK, and test dependencies.
IDE entry point: `org.example.App`.

## Building and testing

```bash
./gradlew :app:test :app:build
```

HTML test report: `app/build/reports/tests/test/index.html`.

## Design patterns and responsibilities

| Student | Patterns | Files |
| --- | --- | --- |
| A | Factory Method, Decorator | `menu/`, `menu/factory/`, `menu/decorator/` |
| B | Singleton, Observer | `order/OrderManager.java`, `order/observer/`; also the order model and statuses |
| C | Builder, Strategy | `order/OrderBuilder.java`, `pricing/`, `pricing/strategy/` |

All paths in the table are relative to `app/src/main/java/org/example/`.
`App.java` integrates the work of all three students.

## Features

- Dishes: Pizza (2000 KZT), Burger (1500 KZT).
- Toppings: extra cheese (+200), extra sauce (+100 KZT).
  Toppings can be combined and repeated.
- Discounts: none or 10%.
- An order can contain multiple dishes. Each order is associated with a table number.
- Statuses: `NEW -> COOKING -> READY -> DELIVERED`.
- The kitchen and customer receive console notifications when an order is registered and whenever its status changes.
- All amounts use `BigDecimal` and are rounded to two decimal places.

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

UML diagrams and a PDF report are not included at this stage.
