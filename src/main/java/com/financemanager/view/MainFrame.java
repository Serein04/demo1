package com.financemanager.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

import com.financemanager.ai.ExpenseAnalyzer;
import com.financemanager.ai.TransactionClassifier;
import com.financemanager.model.BudgetManager;
import com.financemanager.model.Transaction;
import com.financemanager.model.TransactionManager;

/**
 * 主窗口类
 * 提供用户与应用程序交互的图形界面
 */
public class MainFrame extends JFrame {
    private TransactionManager transactionManager;
    private BudgetManager budgetManager;
    private TransactionClassifier classifier;
    private ExpenseAnalyzer analyzer;
    private StartFrame startFrame; // 添加StartFrame引用
    
    // UI组件
    private JTabbedPane tabbedPane;

    public JTabbedPane getTabbedPane() {
        return tabbedPane;
    }
    private JTable transactionTable;
    private DefaultTableModel transactionTableModel;
    private JComboBox<String> categoryComboBox;
    private JComboBox<String> paymentMethodComboBox;
    private JTextField amountField;
    private JTextField descriptionField;
    private JButton addButton;
    private JButton importButton;
    private JButton deleteButton;
    private JButton editButton;
    
    // 日期选择器组件
    private JComboBox<Integer> yearComboBox;
    private JComboBox<Integer> monthComboBox;
    private JComboBox<Integer> dayComboBox;
    
    // 预算设置组件
    private JTextField monthlyBudgetField;
    private JTextField savingsGoalField;
    private JComboBox<String> budgetCategoryComboBox;
    private JTextField categoryBudgetField;
    private JButton setBudgetButton;
    
    // 分析组件
    private JPanel analysisPanel;
    
    // 常量
    private static final String[] PAYMENT_METHODS = {
            "现金", "信用卡", "借记卡", "微信", "支付宝", "其他"
    };
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    /**
     * 构造函数
     */
    public MainFrame(TransactionManager transactionManager, BudgetManager budgetManager,
                     TransactionClassifier classifier, ExpenseAnalyzer analyzer) {
        this.transactionManager = transactionManager;
        this.budgetManager = budgetManager;
        this.classifier = classifier;
        this.analyzer = analyzer;
        
        initUI();
        loadTransactions();
        loadBudgetData();
    }
    
    /**
     * 设置StartFrame引用
     */
    public void setStartFrame(StartFrame startFrame) {
        this.startFrame = startFrame;
    }
    
    /**
     * 初始化UI组件
     */
    private void initUI() {
        setTitle("个人财务管理器");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
        
        // 创建选项卡面板
        tabbedPane = new JTabbedPane();
        
        // 添加交易记录选项卡
        tabbedPane.addTab("交易记录", createTransactionPanel());
        
        // 添加预算管理选项卡
        tabbedPane.addTab("预算管理", createBudgetPanel());
        
        // 添加分析报告选项卡
        tabbedPane.addTab("分析报告", createAnalysisPanel());
        
        // 添加AI助手选项卡
        tabbedPane.addTab("AI助手", createAIAssistantPanel());
        
        // 设置内容面板
        setContentPane(tabbedPane);
    }
    
    /**
     * 返回主界面
     */
    private void returnToMainScreen() {
        if (startFrame != null) {
            this.dispose(); // 关闭当前窗口
            startFrame.setVisible(true); // 显示StartFrame
        } else {
            this.dispose(); // 如果没有StartFrame引用，只关闭当前窗口
        }
    }
    
    /**
     * 创建交易记录面板
     */
    private JPanel createTransactionPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // 添加返回主界面按钮
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton backButton = new JButton("返回主界面");
        backButton.addActionListener(e -> returnToMainScreen());
        topPanel.add(backButton);
        panel.add(topPanel, BorderLayout.NORTH);

        // 创建快速访问面板
        JPanel quickAccessPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        
        // 创建表格模型
        String[] columnNames = {"ID", "金额", "日期", "类别", "描述", "类型", "支付方式"};
        transactionTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 设置表格不可编辑
            }
        };
        
        // 创建表格
        transactionTable = new JTable(transactionTableModel);
        JScrollPane scrollPane = new JScrollPane(transactionTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // 创建输入面板
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(BorderFactory.createTitledBorder("添加交易记录"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // 金额输入
        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(new JLabel("金额:"), gbc);
        
        gbc.gridx = 1;
        amountField = new JTextField(10);
        inputPanel.add(amountField, gbc);
        
        // 日期选择
        gbc.gridx = 0;
        gbc.gridy = 1;
        inputPanel.add(new JLabel("日期:"), gbc);
        
        // 创建日期选择面板
        JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        
        // 年份下拉框
        yearComboBox = new JComboBox<>();
        int currentYear = LocalDate.now().getYear();
        for (int year = currentYear - 10; year <= currentYear + 1; year++) {
            yearComboBox.addItem(year);
        }
        yearComboBox.setSelectedItem(currentYear);
        yearComboBox.addActionListener(e -> {
            // 根据选择的年月更新日期下拉框选项
            int year = (Integer) yearComboBox.getSelectedItem();
            int month = (Integer) monthComboBox.getSelectedItem();
            int daysInMonth = YearMonth.of(year, month).lengthOfMonth();
            
            dayComboBox.removeAllItems();
            for (int day = 1; day <= daysInMonth; day++) {
                dayComboBox.addItem(day);
            }
        });
        datePanel.add(yearComboBox);
        datePanel.add(new JLabel("年"));
        
        // 月份下拉框
        monthComboBox = new JComboBox<>();
        for (int month = 1; month <= 12; month++) {
            monthComboBox.addItem(month);
        }
        monthComboBox.setSelectedItem(LocalDate.now().getMonthValue());
        monthComboBox.addActionListener(e -> {
            // 根据选择的年月更新日期下拉框选项
            int year = (Integer) yearComboBox.getSelectedItem();
            int month = (Integer) monthComboBox.getSelectedItem();
            int daysInMonth = YearMonth.of(year, month).lengthOfMonth();
            
            dayComboBox.removeAllItems();
            for (int day = 1; day <= daysInMonth; day++) {
                dayComboBox.addItem(day);
            }
        });
        datePanel.add(monthComboBox);
        datePanel.add(new JLabel("月"));
        
        // 日期下拉框
        dayComboBox = new JComboBox<>();
        // 初始化日期下拉框选项
        int year = (Integer) yearComboBox.getSelectedItem();
        int month = (Integer) monthComboBox.getSelectedItem();
        int daysInMonth = YearMonth.of(year, month).lengthOfMonth();

        dayComboBox.removeAllItems();
        for (int day = 1; day <= daysInMonth; day++) {
            dayComboBox.addItem(day);
        }
        dayComboBox.setSelectedItem(LocalDate.now().getDayOfMonth());
        datePanel.add(dayComboBox);
        datePanel.add(new JLabel("日"));
        
        gbc.gridx = 1;
        inputPanel.add(datePanel, gbc);
        
        // 类别选择
        gbc.gridx = 0;
        gbc.gridy = 2;
        inputPanel.add(new JLabel("类别:"), gbc);
        
        gbc.gridx = 1;
        categoryComboBox = new JComboBox<>();
        inputPanel.add(categoryComboBox, gbc);
        
        // 描述输入
        gbc.gridx = 0;
        gbc.gridy = 3;
        inputPanel.add(new JLabel("描述:"), gbc);
        
        gbc.gridx = 1;
        descriptionField = new JTextField(20);
        inputPanel.add(descriptionField, gbc);
        
        // 支出/收入选择
        gbc.gridx = 0;
        gbc.gridy = 4;
        inputPanel.add(new JLabel("类型:"), gbc);
        
        gbc.gridx = 1;
        JLabel typeLabel = new JLabel("支出");
        typeLabel.setFont(new Font(typeLabel.getFont().getName(), Font.BOLD, 12));
        inputPanel.add(typeLabel, gbc);
        
        // 更新类别下拉框
        updateCategoryComboBox(true);
        
        // 支付方式选择
        JPanel paymentMethodPanel = new JPanel(new GridBagLayout());
        gbc.gridx = 0;
        gbc.gridy = 5;
        paymentMethodPanel.add(new JLabel("支付方式:"), gbc);
        
        gbc.gridx = 1;
        paymentMethodComboBox = new JComboBox<>(PAYMENT_METHODS);
        paymentMethodPanel.add(paymentMethodComboBox, gbc);
        inputPanel.add(paymentMethodPanel, gbc);
        
        // 根据交易类型控制支付方式组件的显示
        paymentMethodPanel.setVisible(true); // 默认显示（支出类型）
        
        // 支出按钮
        JButton expenseButton = new JButton("支出");
        expenseButton.setPreferredSize(new Dimension(120, 40));
        expenseButton.setFont(new Font(expenseButton.getFont().getName(), Font.BOLD, 14));
        expenseButton.setBackground(new Color(255, 99, 71));
        expenseButton.setForeground(Color.BLACK);
        expenseButton.addActionListener(e -> {
            typeLabel.setText("支出");
            updateCategoryComboBox(true);
            paymentMethodPanel.setVisible(true);
        });
        quickAccessPanel.add(expenseButton);
        
        // 收入按钮
        JButton incomeButton = new JButton("收入");
        incomeButton.setPreferredSize(new Dimension(120, 40));
        incomeButton.setFont(new Font(incomeButton.getFont().getName(), Font.BOLD, 14));
        incomeButton.setBackground(new Color(60, 179, 113));
        incomeButton.setForeground(Color.BLACK);
        incomeButton.addActionListener(e -> {
            typeLabel.setText("收入");
            updateCategoryComboBox(false);
            paymentMethodPanel.setVisible(false);
        });
        quickAccessPanel.add(incomeButton);
        
        // 创建按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        
        // 添加按钮
        addButton = new JButton("添加");
        addButton.addActionListener(e -> addTransaction());
        buttonPanel.add(addButton);
        
        // 删除按钮
        deleteButton = new JButton("删除");
        deleteButton.addActionListener(e -> deleteTransaction());
        buttonPanel.add(deleteButton);
        
        // 编辑按钮
        editButton = new JButton("编辑");
        editButton.addActionListener(e -> editTransaction());
        buttonPanel.add(editButton);
        
        // 导入CSV按钮
        importButton = new JButton("导入CSV");
        importButton.addActionListener(e -> importFromCSV());
        buttonPanel.add(importButton);

        // 添加输入面板和按钮面板
        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.add(inputPanel, BorderLayout.CENTER);
        controlPanel.add(buttonPanel, BorderLayout.NORTH);
        controlPanel.add(quickAccessPanel, BorderLayout.SOUTH);
        
        panel.add(controlPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    /**
     * 创建预算管理面板
     */
    private JPanel createBudgetPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // 添加返回主界面按钮
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton backButton = new JButton("返回主界面");
        backButton.addActionListener(e -> returnToMainScreen());
        topPanel.add(backButton);
        panel.add(topPanel, BorderLayout.NORTH);
        
        // 创建设置面板
        JPanel settingsPanel = new JPanel(new GridBagLayout());
        settingsPanel.setBorder(BorderFactory.createTitledBorder("预算设置"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // 月度预算
        gbc.gridx = 0;
        gbc.gridy = 0;
        settingsPanel.add(new JLabel("月度总预算:"), gbc);
        
        gbc.gridx = 1;
        monthlyBudgetField = new JTextField(10);
        settingsPanel.add(monthlyBudgetField, gbc);
        
        gbc.gridx = 2;
        JButton setMonthlyBudgetButton = new JButton("设置");
        setMonthlyBudgetButton.addActionListener(e -> setMonthlyBudget());
        settingsPanel.add(setMonthlyBudgetButton, gbc);
        
        // 储蓄目标
        gbc.gridx = 0;
        gbc.gridy = 1;
        settingsPanel.add(new JLabel("储蓄目标:"), gbc);
        
        gbc.gridx = 1;
        savingsGoalField = new JTextField(10);
        settingsPanel.add(savingsGoalField, gbc);
        
        gbc.gridx = 2;
        JButton setSavingsGoalButton = new JButton("设置");
        setSavingsGoalButton.addActionListener(e -> setSavingsGoal());
        settingsPanel.add(setSavingsGoalButton, gbc);
        
        // 类别预算
        gbc.gridx = 0;
        gbc.gridy = 2;
        settingsPanel.add(new JLabel("类别:"), gbc);
        
        gbc.gridx = 1;
        budgetCategoryComboBox = new JComboBox<>();
        for (String category : classifier.getExpenseCategories()) {
            budgetCategoryComboBox.addItem(category);
        }
        settingsPanel.add(budgetCategoryComboBox, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 3;
        settingsPanel.add(new JLabel("类别预算:"), gbc);
        
        gbc.gridx = 1;
        categoryBudgetField = new JTextField(10);
        settingsPanel.add(categoryBudgetField, gbc);
        
        gbc.gridx = 2;
        setBudgetButton = new JButton("设置");
        setBudgetButton.addActionListener(e -> setCategoryBudget());
        settingsPanel.add(setBudgetButton, gbc);
        
        // 创建预算显示面板
        JPanel budgetDisplayPanel = new JPanel(new BorderLayout());
        budgetDisplayPanel.setBorder(BorderFactory.createTitledBorder("当前预算"));
        
        // 创建表格模型
        String[] columnNames = {"类别", "预算金额", "当前支出", "剩余金额", "状态"};
        DefaultTableModel budgetTableModel = new DefaultTableModel(columnNames, 0);
        
        // 创建表格
        JTable budgetTable = new JTable(budgetTableModel);
        JScrollPane scrollPane = new JScrollPane(budgetTable);
        budgetDisplayPanel.add(scrollPane, BorderLayout.CENTER);
        
        // 添加面板
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(settingsPanel, BorderLayout.NORTH);
        centerPanel.add(budgetDisplayPanel, BorderLayout.CENTER);
        panel.add(centerPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * 创建分析报告面板
     */
    private JPanel createAnalysisPanel() {
        analysisPanel = new JPanel(new BorderLayout());
        analysisPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // 添加返回主界面按钮
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton backButton = new JButton("返回主界面");
        backButton.addActionListener(e -> returnToMainScreen());
        topPanel.add(backButton);
        
        JPanel controlPanel = new JPanel();
        JButton analyzeButton = new JButton("生成分析报告");
        analyzeButton.addActionListener(e -> generateAnalysisReport());
        controlPanel.add(analyzeButton);
        
        // 组合顶部面板
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(topPanel, BorderLayout.NORTH);
        northPanel.add(controlPanel, BorderLayout.CENTER);
        
        analysisPanel.add(northPanel, BorderLayout.NORTH);
        
        // 创建报告面板（初始为空）
        JPanel reportPanel = new JPanel();
        reportPanel.setLayout(new BoxLayout(reportPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(reportPanel);
        analysisPanel.add(scrollPane, BorderLayout.CENTER);
        
        return analysisPanel;
    }
    
    /**
     * 创建AI助手面板
     */
    private JPanel createAIAssistantPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // 添加返回主界面按钮
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton backButton = new JButton("返回主界面");
        backButton.addActionListener(e -> returnToMainScreen());
        topPanel.add(backButton);
        panel.add(topPanel, BorderLayout.NORTH);
        
        // 创建AI助手面板

        return panel;
    }
    
    /**
     * 更新类别下拉框
     */
    private void updateCategoryComboBox(boolean isExpense) {
        categoryComboBox.removeAllItems();
        List<String> categories;
        if (isExpense) {
            categories = classifier.getExpenseCategories();
        } else {
            categories = classifier.getIncomeCategories();
        }
        for (String category : categories) {
            categoryComboBox.addItem(category);
        }
    }
    
    /**
     * 加载交易记录到表格
     */
    private void loadTransactions() {
        // 清空表格
        transactionTableModel.setRowCount(0);
        
        // 获取所有交易记录
        List<Transaction> transactions = transactionManager.getAllTransactions();
        
        // 添加到表格
        for (Transaction t : transactions) {
            Object[] rowData = {
                    t.getId(),
                    String.format("%.2f", t.getAmount()),
                    t.getDate().format(DATE_FORMATTER),
                    t.getCategory(),
                    t.getDescription(),
                    t.isExpense() ? "支出" : "收入",
                    t.getPaymentMethod()
            };
            transactionTableModel.addRow(rowData);
        }
    }
    
    /**
     * 加载预算数据
     */
    private void loadBudgetData() {
        // 显示月度预算和储蓄目标
        monthlyBudgetField.setText(String.format("%.2f", budgetManager.getMonthlyBudget()));
        savingsGoalField.setText(String.format("%.2f", budgetManager.getSavingsGoal()));
        
        // 显示当前选中类别的预算
        String selectedCategory = (String) budgetCategoryComboBox.getSelectedItem();
        if (selectedCategory != null) {
            double budget = budgetManager.getCategoryBudget(selectedCategory);
            categoryBudgetField.setText(String.format("%.2f", budget));
        }
    }
    
    /**
     * 添加交易记录
     */
    private void addTransaction() {
        try {
            // 获取输入值
            double amount = Double.parseDouble(amountField.getText());
            int year = (Integer) yearComboBox.getSelectedItem();
            int month = (Integer) monthComboBox.getSelectedItem();
            int day = (Integer) dayComboBox.getSelectedItem();
            LocalDate localDate = LocalDate.of(year, month, day);
            String category = (String) categoryComboBox.getSelectedItem();
            String description = descriptionField.getText();
            boolean isExpense = true; // 默认为支出
            String paymentMethod = (String) paymentMethodComboBox.getSelectedItem();
            
            // 创建交易记录
            Transaction transaction = new Transaction(
                    amount, localDate, category, description, isExpense, paymentMethod);
            
            // 添加到管理器
            transactionManager.addTransaction(transaction);
            
            try {
                // 使用AI进行分类
                String aiCategory = classifier.classifyTransaction(transaction);
                if (!aiCategory.equals(category)) {
                    classifier.learnFromUserCorrection(transaction, aiCategory, category);
                }
                // 显示成功消息
                JOptionPane.showMessageDialog(this, "交易记录添加成功", "成功", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                // AI分类过程中出现异常
                String errorMessage = e.getMessage() != null ? e.getMessage() : "未知错误";
                JOptionPane.showMessageDialog(this, 
                    String.format("交易记录已添加,但AI分类出现错误:%s\n请检查交易描述是否完整。", errorMessage),
                    "AI分类错误", 
                    JOptionPane.WARNING_MESSAGE);
            }
            
            // 刷新表格
            loadTransactions();
            
            // 清空输入框
            amountField.setText("");
            descriptionField.setText("");
            
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "请输入有效的金额", "输入错误", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "添加交易记录时出错: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * 删除交易记录
     */
    private void deleteTransaction() {
        int selectedRow = transactionTable.getSelectedRow();
        if (selectedRow >= 0) {
            String id = (String) transactionTableModel.getValueAt(selectedRow, 0);
            int confirm = JOptionPane.showConfirmDialog(this, "确定要删除选中的交易记录吗？", "确认删除", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                transactionManager.removeTransaction(id);
                loadTransactions();
            }
        } else {
            JOptionPane.showMessageDialog(this, "请先选择要删除的交易记录", "提示", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    /**
     * 编辑交易记录
     */
    private void editTransaction() {
        int selectedRow = transactionTable.getSelectedRow();
        if (selectedRow >= 0) {
            String id = (String) transactionTableModel.getValueAt(selectedRow, 0);
            // 获取选中的交易记录
            List<Transaction> transactions = transactionManager.getAllTransactions();
            Transaction selectedTransaction = null;
            for (Transaction t : transactions) {
                if (t.getId().equals(id)) {
                    selectedTransaction = t;
                    break;
                }
            }
            
            if (selectedTransaction != null) {
                // 显示编辑对话框（简化版，实际应用中可以创建一个更复杂的编辑对话框）
                try {
                    String amountStr = JOptionPane.showInputDialog(this, "金额:", selectedTransaction.getAmount());
                    if (amountStr != null) {
                        double amount = Double.parseDouble(amountStr);
                        selectedTransaction.setAmount(amount);
                        
                        // 更新交易记录
                        transactionManager.updateTransaction(selectedTransaction);
                        loadTransactions();
                    }
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(this, "请输入有效的金额", "输入错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        } else {
            JOptionPane.showMessageDialog(this, "请先选择要编辑的交易记录", "提示", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    /**
     * 从CSV文件导入交易记录
     */
    private void importFromCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("CSV文件", "csv"));
        int result = fileChooser.showOpenDialog(this);
        
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            int importedCount = transactionManager.importFromCSV(selectedFile.getAbsolutePath());
            JOptionPane.showMessageDialog(this, "成功导入" + importedCount + "条交易记录", "导入成功", JOptionPane.INFORMATION_MESSAGE);
            loadTransactions();
        }
    }
    
    /**
     * 设置月度总预算
     */
    private void setMonthlyBudget() {
        try {
            double amount = Double.parseDouble(monthlyBudgetField.getText());
            budgetManager.setMonthlyBudget(amount);
            JOptionPane.showMessageDialog(this, "月度预算设置成功", "成功", JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "请输入有效的金额", "输入错误", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * 设置储蓄目标
     */
    private void setSavingsGoal() {
        try {
            double amount = Double.parseDouble(savingsGoalField.getText());
            budgetManager.setSavingsGoal(amount);
            JOptionPane.showMessageDialog(this, "储蓄目标设置成功", "成功", JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "请输入有效的金额", "输入错误", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * 设置类别预算
     */
    private void setCategoryBudget() {
        try {
            String category = (String) budgetCategoryComboBox.getSelectedItem();
            double amount = Double.parseDouble(categoryBudgetField.getText());
            
            if (category != null) {
                budgetManager.setCategoryBudget(category, amount);
                JOptionPane.showMessageDialog(this, "类别预算设置成功", "成功", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "请输入有效的金额", "输入错误", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * 生成分析报告
     */
    private void generateAnalysisReport() {
      
    }
}
