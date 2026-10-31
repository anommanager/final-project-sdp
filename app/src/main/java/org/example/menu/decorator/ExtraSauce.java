package org.example.menu.decorator;

import java.math.BigDecimal;
import org.example.menu.MenuItem;

public class ExtraSauce extends ToppingDecorator {
    public ExtraSauce(MenuItem item) {
        super(item, "extra sauce", new BigDecimal("100.00"));
    }
}
