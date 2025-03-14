package com.financemanager;

import javax.swing.UnsupportedLookAndFeelException;

import com.financemanager.ai.ExpenseAnalyzer;
import com.financemanager.ai.TransactionClassifier;
import com.financemanager.model.BudgetManager;
import com.financemanager.model.TransactionManager;
import com.financemanager.view.StartFrame;

/**
 * 个人财务管理器应用程序入口类
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("启动个人财务管理器...");
        
        // 初始化数据模型
        TransactionManager transactionManager = new TransactionManager();
        BudgetManager budgetManager = new BudgetManager();
        
        // 初始化AI组件
        TransactionClassifier classifier = new TransactionClassifier();
        ExpenseAnalyzer analyzer = new ExpenseAnalyzer(classifier);
        
        // 启动GUI界面
        javax.swing.SwingUtilities.invokeLater(() -> {
            try {
                // 设置本地化的外观
                javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException e) {
            }
            
            // 创建并显示启动界面
            StartFrame startFrame = new StartFrame(transactionManager, budgetManager, classifier, analyzer);
            startFrame.setVisible(true);
        });
    }
}
