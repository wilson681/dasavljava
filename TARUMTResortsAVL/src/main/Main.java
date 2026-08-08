package main;

import boundary.MainMenuCLI;
import control.MainMenuControl;

/**
 * Main.java - Application entry point.
 *
 * @author 某某
 */
public class Main {

    public static void main(String[] args) {
        MainMenuCLI mainMenuCLI = new MainMenuCLI();
        MainMenuControl mainMenuControl = new MainMenuControl(mainMenuCLI);
        mainMenuControl.run();
    }
}