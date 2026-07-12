package control;

import boundary.MainMenuCLI;

/**
 * 控制主画面的启动流程。
 */
public class MainMenuControl {

    private final MainMenuCLI mainMenuCLI;

    /**
     * @param mainMenuCLI 负责显示主画面的 Boundary
     */
    public MainMenuControl(MainMenuCLI mainMenuCLI) {
        if (mainMenuCLI == null) {
            throw new IllegalArgumentException("mainMenuCLI cannot be null");
        }
        this.mainMenuCLI = mainMenuCLI;
    }

    /**
     * 启动主画面。
     */
    public void run() {
        mainMenuCLI.displayWelcome();
    }
}
