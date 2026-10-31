package org.example.menu.decorator;

import java.math.BigDecimal;
import org.example.menu.MenuItem;

public class ExtraCheese extends ToppingDecorator {
    public ExtraCheese(MenuItem item) {
        super(item, "extra cheese", new BigDecimal("200.00"));
    }
}
