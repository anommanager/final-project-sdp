package org.example.menu.factory;

import org.example.menu.MenuItem;
import org.example.menu.Pizza;

public class PizzaFactory extends MenuItemFactory {
    @Override
    public MenuItem createItem() {
        return new Pizza();
    }
}
