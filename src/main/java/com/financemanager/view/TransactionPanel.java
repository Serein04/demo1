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
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

import com.financemanager.ai.TransactionClassifier;
import com.financemanager.model.Transaction;
import com.financemanager.model.TransactionManager;

/**
 * 交易记录面板类
 * 包含交易记录选项卡的所有UI和逻辑
 */
public class TransactionPanel extends JPanel {

    private final TransactionManager transactionManager;
    private final TransactionClassifier classifier;
    private final MainFrame mainFrame; // 引用主窗口以便调用返回等方法

    // UI组件
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
    private JLabel typeLabel; // 移到类成员变量

    // 日期选择器组件
    private JComboBox<Integer> yearComboBox;
    private JComboBox<Integer> monthComboBox;
    private JComboBox<Integer> dayComboBox;

    // 常量
    private static final String[] PAYMENT_METHODS = {
            "现金", "信用卡", "借记卡", "微信", "支付宝", "其他"
    };
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public TransactionPanel(TransactionManager transactionManager, TransactionClassifier classifier, MainFrame mainFrame) {
        this.transactionManager = transactionManager;
        this.classifier = classifier;
        this.mainFrame = mainFrame; // 保存主窗口引用

        initComponents();
        loadTransactions(); // 初始化时加载数据
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // 添加返回主界面按钮
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton backButton = new JButton("返回主界面");
        backButton.addActionListener(e -> mainFrame.returnToMainScreen()); // 使用 mainFrame 引用
        topPanel.add(backButton);
        add(topPanel, BorderLayout.NORTH);

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
        add(scrollPane, BorderLayout.CENTER);

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
        yearComboBox.addActionListener(e -> updateDaysInMonth());
        datePanel.add(yearComboBox);
        datePanel.add(new JLabel("年"));

        // 月份下拉框
        monthComboBox = new JComboBox<>();
        for (int month = 1; month <= 12; month++) {
            monthComboBox.addItem(month);
        }
        monthComboBox.setSelectedItem(LocalDate.now().getMonthValue());
        monthComboBox.addActionListener(e -> updateDaysInMonth());
        datePanel.add(monthComboBox);
        datePanel.add(new JLabel("月"));

        // 日期下拉框
        dayComboBox = new JComboBox<>();
        updateDaysInMonth(); // 初始化日期下拉框选项
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
        typeLabel = new JLabel("支出"); // 初始化 typeLabel
        typeLabel.setFont(new Font(typeLabel.getFont().getName(), Font.BOLD, 12));
        inputPanel.add(typeLabel, gbc);

        // 更新类别下拉框
        updateCategoryComboBox(true);

        // 支付方式选择
        JPanel paymentMethodPanel = new JPanel(new GridBagLayout());
        GridBagConstraints pmg = new GridBagConstraints(); // Use separate GBC for this sub-panel
        pmg.insets = new Insets(0,0,0,0); // No insets needed here
        pmg.gridx = 0;
        pmg.gridy = 0;
        paymentMethodPanel.add(new JLabel("支付方式:"), pmg);

        pmg.gridx = 1;
        paymentMethodComboBox = new JComboBox<>(PAYMENT_METHODS);
        paymentMethodPanel.add(paymentMethodComboBox, pmg);

        // 创建一个空白面板，与支付方式面板具有相同的尺寸
        JPanel emptyPanel = new JPanel();
        emptyPanel.setPreferredSize(paymentMethodPanel.getPreferredSize());

        // 添加支付方式面板和空白面板到同一位置 (使用 inputPanel 的 gbc)
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2; // Span across two columns
        inputPanel.add(paymentMethodPanel, gbc);
        inputPanel.add(emptyPanel, gbc);

        // 默认显示支付方式面板，隐藏空白面板
        paymentMethodPanel.setVisible(true);
        emptyPanel.setVisible(false);

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
            emptyPanel.setVisible(false);
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
            emptyPanel.setVisible(true);
        });
        quickAccessPanel.add(incomeButton);

        // 创建按钮面板（居中显示）
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));

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
        controlPanel.add(quickAccessPanel, BorderLayout.NORTH); // 支出/收入按钮移到上方
        controlPanel.add(buttonPanel, BorderLayout.SOUTH); // 操作按钮移到下方

        add(controlPanel, BorderLayout.SOUTH);
    }

    /**
     * 更新日期下拉框中的天数
     */
    private void updateDaysInMonth() {
        int year = (Integer) yearComboBox.getSelectedItem();
        int month = (Integer) monthComboBox.getSelectedItem();
        int daysInMonth = YearMonth.of(year, month).lengthOfMonth();

        // 保存当前选中的日期，如果有效的话
        Object selectedDayObj = dayComboBox.getSelectedItem();
        int selectedDay = (selectedDayObj instanceof Integer) ? (Integer) selectedDayObj : 1;

        dayComboBox.removeAllItems();
        for (int day = 1; day <= daysInMonth; day++) {
            dayComboBox.addItem(day);
        }

        // 尝试恢复之前的选中日期，如果新月份中还存在的话
        if (selectedDay <= daysInMonth) {
            dayComboBox.setSelectedItem(selectedDay);
        } else {
            dayComboBox.setSelectedItem(daysInMonth); // 如果日期超出，则选中最后一天
        }
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
    public void loadTransactions() { // 改为 public 以便 MainFrame 调用刷新
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
        // 通知主窗口更新可能依赖交易数据的其他面板（如图表）
        mainFrame.refreshBudgetPanel();
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
            boolean isExpense = typeLabel.getText().equals("支出"); // 根据 typeLabel 判断
            String paymentMethod = isExpense ? (String) paymentMethodComboBox.getSelectedItem() : ""; // 收入无支付方式

            // 创建交易记录
            Transaction transaction = new Transaction(
                    amount, localDate, category, description, isExpense, paymentMethod);

            // 添加到管理器
            transactionManager.addTransaction(transaction);

            // AI 分类 (可选)
            try {
                String aiCategory = classifier.classifyTransaction(transaction);
                if (!aiCategory.equals(category)) {
                    // 可以选择提示用户或自动修正，这里仅作学习
                    classifier.learnFromUserCorrection(transaction, aiCategory, category);
                }
                 JOptionPane.showMessageDialog(this, "交易记录添加成功", "成功", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception aiEx) { // 捕获更广泛的AI异常
                 JOptionPane.showMessageDialog(this,
                    String.format("交易记录已添加,但AI分类出现错误:%s\n请检查交易描述是否完整。", aiEx.getMessage()),
                    "AI分类警告",
                    JOptionPane.WARNING_MESSAGE);
            }


            // 刷新表格
            loadTransactions(); // loadTransactions 内部会调用 mainFrame.refreshBudgetPanel()

            // 清空输入框
            amountField.setText("");
            descriptionField.setText("");

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "请输入有效的金额", "输入错误", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) { // 捕获其他可能的异常
            JOptionPane.showMessageDialog(this, "添加交易记录时出错: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace(); // 打印堆栈信息以便调试
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
                if (transactionManager.removeTransaction(id)) {
                    loadTransactions(); // loadTransactions 内部会调用 mainFrame.refreshBudgetPanel()
                } else {
                     JOptionPane.showMessageDialog(this, "删除失败，未找到该记录", "错误", JOptionPane.ERROR_MESSAGE);
                }
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
            // 获取选中的交易记录 (从管理器获取最新数据)
            Transaction selectedTransaction = transactionManager.getAllTransactions().stream()
                                                    .filter(t -> t.getId().equals(id))
                                                    .findFirst()
                                                    .orElse(null);

            if (selectedTransaction != null) {
                // 显示编辑对话框
                TransactionEditDialog dialog = new TransactionEditDialog(mainFrame, selectedTransaction, classifier); // 父窗口改为 mainFrame
                dialog.setVisible(true);

                if (dialog.isConfirmed()) {
                    // 获取编辑后的交易对象 (对话框内部修改了传入的对象)
                    Transaction updatedTransaction = dialog.getTransaction(); // 修正方法调用
                    if (transactionManager.updateTransaction(updatedTransaction)) {
                        loadTransactions(); // loadTransactions 内部会调用 mainFrame.refreshBudgetPanel()
                    } else {
                         JOptionPane.showMessageDialog(this, "更新失败，未找到该记录", "错误", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } else {
                 JOptionPane.showMessageDialog(this, "无法加载选中的交易记录", "错误", JOptionPane.ERROR_MESSAGE);
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
        int result = fileChooser.showOpenDialog(this); // 父组件是当前 Panel

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                int importedCount = transactionManager.importFromCSV(selectedFile.getAbsolutePath());
                JOptionPane.showMessageDialog(this, "成功导入" + importedCount + "条交易记录", "导入成功", JOptionPane.INFORMATION_MESSAGE);
                loadTransactions(); // loadTransactions 内部会调用 mainFrame.refreshBudgetPanel()
            } catch (Exception e) {
                 JOptionPane.showMessageDialog(this, "导入CSV文件时出错: " + e.getMessage(), "导入错误", JOptionPane.ERROR_MESSAGE);
                 e.printStackTrace();
            }
        }
    }
}
