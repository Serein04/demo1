package com.financemanager;

// Removed Swing imports and AI/Model imports as they are not directly used here anymore for startup.
// Business logic initialization will be handled within the JavaFX application structure if needed globally,
// or per-module. For now, Main.java just delegates to MainJavaFX.

/**
 * 个人财务管理器应用程序入口类
 * Now launches the JavaFX application.
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("启动个人财务管理器 (JavaFX)...");
        // 调用 JavaFX 应用程序的 main 方法
        MainJavaFX.main(args);
    }
}
