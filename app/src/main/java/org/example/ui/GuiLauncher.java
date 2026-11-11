package org.example.ui;

import javafx.application.Application;

/** Entry point that does not extend Application, so JavaFX works from the classpath. */
public final class GuiLauncher {
    private GuiLauncher() {
    }

    public static void main(String[] args) {
        Application.launch(RestaurantFxApp.class, args);
    }
}
