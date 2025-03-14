package com.financemanager.view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import com.financemanager.ai.ExpenseAnalyzer;
import com.financemanager.ai.TransactionClassifier;
import com.financemanager.model.BudgetManager;
import com.financemanager.model.TransactionManager;

public class StartFrame extends JFrame {
    private final TransactionManager transactionManager;
    private final BudgetManager budgetManager;
    private final TransactionClassifier classifier;
    private final ExpenseAnalyzer analyzer;

    // 圆形按钮类
    private class RoundButton extends JButton {
        public RoundButton(String label) {
            super(label);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (getModel().isArmed()) {
                g.setColor(Color.LIGHT_GRAY);
            } else {
                g.setColor(getBackground());
            }
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.fill(new Ellipse2D.Double(0, 0, getWidth() - 1, getHeight() - 1));
            super.paintComponent(g);
        }

        @Override
        protected void paintBorder(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getForeground());
            g2.draw(new Ellipse2D.Double(0, 0, getWidth() - 1, getHeight() - 1));
        }

        @Override
        public boolean contains(int x, int y) {
            return new Ellipse2D.Double(0, 0, getWidth() - 1, getHeight() - 1).contains(x, y);
        }
    }

    public StartFrame(TransactionManager transactionManager, BudgetManager budgetManager,
                      TransactionClassifier classifier, ExpenseAnalyzer analyzer) {
        this.transactionManager = transactionManager;
        this.budgetManager = budgetManager;
        this.classifier = classifier;
        this.analyzer = analyzer;

        initUI();
    }

    private void initUI() {
        setTitle("个人财务管理器");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);

        // 创建主面板
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(new Color(240, 240, 240));

        // 创建四个圆形按钮
        RoundButton transactionButton = createButton("交易记录", new Color(100, 181, 246));
        RoundButton budgetButton = createButton("预算管理", new Color(129, 199, 132));
        RoundButton analysisButton = createButton("分析报告", new Color(239, 154, 154));
        RoundButton aiButton = createButton("AI助手", new Color(206, 147, 216));

        // 设置按钮布局
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        gbc.gridx = 0;
        gbc.gridy = 0;
        mainPanel.add(transactionButton, gbc);

        gbc.gridx = 1;
        mainPanel.add(budgetButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        mainPanel.add(analysisButton, gbc);

        gbc.gridx = 1;
        mainPanel.add(aiButton, gbc);

        setContentPane(mainPanel);

        // 添加按钮点击事件
        transactionButton.addActionListener(e -> openModule("交易记录"));
        budgetButton.addActionListener(e -> openModule("预算管理"));
        analysisButton.addActionListener(e -> openModule("分析报告"));
        aiButton.addActionListener(e -> openModule("AI助手"));
    }

    private RoundButton createButton(String text, Color color) {
        RoundButton button = new RoundButton(text);
        button.setPreferredSize(new Dimension(150, 150));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("微软雅黑", Font.BOLD, 16));
        return button;
    }

    private void openModule(String moduleName) {
        MainFrame moduleFrame = new MainFrame(transactionManager, budgetManager, classifier, analyzer);
        
        // 设置StartFrame引用
        moduleFrame.setStartFrame(this);
        
        // 根据选择的模块设置初始选项卡
        switch (moduleName) {
            case "交易记录":
                moduleFrame.getTabbedPane().setSelectedIndex(0);
                break;
            case "预算管理":
                moduleFrame.getTabbedPane().setSelectedIndex(1);
                break;
            case "分析报告":
                moduleFrame.getTabbedPane().setSelectedIndex(2);
                break;
            case "AI助手":
                moduleFrame.getTabbedPane().setSelectedIndex(3);
                break;
        }

        moduleFrame.setVisible(true);
        this.setVisible(false);
    }
}
