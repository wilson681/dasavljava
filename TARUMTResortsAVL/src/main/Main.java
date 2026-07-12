package main;

import boundary.MainMenuCLI;
import control.MainMenuControl;

/**
 * 程序唯一入口，负责组装并启动主画面。
 *
 * @author TODO：提交前替换成实际负责组员姓名
 */
public class Main {

    private Main() {
    }

    /**
     * 启动系统。
     *
     * @param args 未使用
     */
    public static void main(String[] args) {
        MainMenuCLI mainMenuCLI = new MainMenuCLI();
        MainMenuControl mainMenuControl = new MainMenuControl(mainMenuCLI);
        mainMenuControl.run();
    }
}
