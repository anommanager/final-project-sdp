package org.example.menu.factory;

import org.example.menu.Burger;
import org.example.menu.MenuItem;

public class BurgerFactory extends MenuItemFactory {
    @Override
    public MenuItem createItem() {
        return new Burger();
    }
}
