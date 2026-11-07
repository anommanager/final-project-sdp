package org.example.menu;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import org.example.menu.decorator.ExtraCheese;
import org.example.menu.decorator.ExtraSauce;
import org.example.menu.factory.BurgerFactory;
import org.example.menu.factory.PizzaFactory;
import org.junit.jupiter.api.Test;

class MenuItemTest {
    @Test
    void factoriesCreateIndependentDishesWithExpectedProperties() {
        PizzaFactory pizzaFactory = new PizzaFactory();
        BurgerFactory burgerFactory = new BurgerFactory();
        MenuItem pizza = pizzaFactory.createItem();
        MenuItem burger = burgerFactory.createItem();

        assertInstanceOf(Pizza.class, pizza);
        assertInstanceOf(Burger.class, burger);
        assertNotSame(pizza, pizzaFactory.createItem());
        assertNotSame(burger, burgerFactory.createItem());
        assertEquals("Pizza", pizza.getName());
        assertEquals("Pizza with tomato and mozzarella", pizza.getDescription());
        assertEquals(new BigDecimal("2000.00"), pizza.getPrice());
        assertEquals("Burger", burger.getName());
        assertEquals("Burger with beef and lettuce", burger.getDescription());
        assertEquals(new BigDecimal("1500.00"), burger.getPrice());
    }

    @Test
    void nestedToppingsPreserveTheOriginalDish() {
        MenuItem base = new PizzaFactory().createItem();
        MenuItem cheese = new ExtraCheese(base);
        MenuItem decorated = new ExtraSauce(new ExtraSauce(cheese));

        assertEquals("Pizza", base.getName());
        assertEquals(new BigDecimal("2000.00"), base.getPrice());
        assertEquals("Pizza + extra cheese", cheese.getName());
        assertEquals(new BigDecimal("2200.00"), cheese.getPrice());
        assertEquals("Pizza + extra cheese + extra sauce + extra sauce", decorated.getName());
        assertEquals("Pizza with tomato and mozzarella, extra cheese, extra sauce, extra sauce",
                decorated.getDescription());
        assertEquals(new BigDecimal("2400.00"), decorated.getPrice());
    }

    @Test
    void toppingsRejectMissingDishes() {
        assertThrows(NullPointerException.class, () -> new ExtraCheese(null));
        assertThrows(NullPointerException.class, () -> new ExtraSauce(null));
    }
}
