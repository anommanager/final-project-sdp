package org.example.ui;

import javafx.application.Application;

public final class GuiLauncher {
    private GuiLauncher() {
    }

    public static void main(String[] args) {
        Application.launch(RestaurantFxApp.class, args);
    }
}
