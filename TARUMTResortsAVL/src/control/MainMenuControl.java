package control;

import boundary.MainMenuCLI;

/**
 * MainMenuControl.java - Controls the main screen startup flow.
 *
 * @author Wilson
 */
public class MainMenuControl {

    private final MainMenuCLI mainMenuCLI;

   /**
     * @param mainMenuCLI the boundary responsible for displaying the main screen
     */
    public MainMenuControl(MainMenuCLI mainMenuCLI) {
        if (mainMenuCLI == null) {
            throw new IllegalArgumentException("mainMenuCLI cannot be null");
        }
        this.mainMenuCLI = mainMenuCLI;
    }

 /**
     * Launches the main screen.
     */
    public void run() {
        mainMenuCLI.displayWelcome();
    }
}
