package com.financemanager.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.YearMonth;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.financemanager.ai.TransactionClassifier;
import com.financemanager.model.Transaction;

/**
 * 交易记录编辑对话框
 * 用于编辑交易记录的所有属性
 */
public class TransactionEditDialog extends JDialog {
    private final Transaction transaction;
    private final TransactionClassifier classifier;
    
    private JTextField amountField;
    private JComboBox<Integer> yearComboBox;
    private JComboBox<Integer> monthComboBox;
    private JComboBox<Integer> dayComboBox;
    private JComboBox<String> categoryComboBox;
    private JTextField descriptionField;
    private JComboBox<String> typeComboBox;
    private JComboBox<String> paymentMethodComboBox;

    private JLabel aiSuggestionValueLabel; // Added for AI suggestion
    private JButton adoptAiButton;         // Added button to adopt AI suggestion
    private String currentAiSuggestion;    // To store the current AI suggestion
    
    private boolean confirmed = false;
    
    // 常量
    private static final String[] PAYMENT_METHODS = {
            "现金", "信用卡", "借记卡", "微信", "支付宝", "其他"
    };
    
    private static final String[] TRANSACTION_TYPES = {
            "支出", "收入"
    };
    
    /**
     * 构造函数
     */
    public TransactionEditDialog(JFrame parent, Transaction transaction, TransactionClassifier classifier) {
        super(parent, "编辑交易记录", true);
        this.transaction = transaction;
        this.classifier = classifier;
        
        initUI();
        loadTransactionData();
        
        pack();
        setLocationRelativeTo(parent);
    }
    
    /**
     * 初始化UI组件
     */
    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // 创建表单面板
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // 金额输入
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("金额:"), gbc);
        
        gbc.gridx = 1;
        amountField = new JTextField(10);
        formPanel.add(amountField, gbc);
        
        // 日期选择
        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(new JLabel("日期:"), gbc);
        
        // 创建日期选择面板
        JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        
        // 年份下拉框
        yearComboBox = new JComboBox<>();
        int currentYear = LocalDate.now().getYear();
        for (int year = currentYear - 10; year <= currentYear + 1; year++) {
            yearComboBox.addItem(year);
        }
        yearComboBox.setSelectedItem(currentYear);
        yearComboBox.addActionListener(e -> updateDayComboBox());
        datePanel.add(yearComboBox);
        datePanel.add(new JLabel("年"));
        
        // 月份下拉框
        monthComboBox = new JComboBox<>();
        for (int month = 1; month <= 12; month++) {
            monthComboBox.addItem(month);
        }
        monthComboBox.setSelectedItem(LocalDate.now().getMonthValue());
        monthComboBox.addActionListener(e -> updateDayComboBox());
        datePanel.add(monthComboBox);
        datePanel.add(new JLabel("月"));
        
        // 日期下拉框
        dayComboBox = new JComboBox<>();
        updateDayComboBox(); // 初始化日期选项
        datePanel.add(dayComboBox);
        datePanel.add(new JLabel("日"));
        
        gbc.gridx = 1;
        formPanel.add(datePanel, gbc);
        
        // 交易类型选择
        gbc.gridx = 0;
        gbc.gridy = 2;
        formPanel.add(new JLabel("类型:"), gbc);
        
        gbc.gridx = 1;
        typeComboBox = new JComboBox<>(TRANSACTION_TYPES);
        typeComboBox.addActionListener(e -> updateCategoryComboBox());
        formPanel.add(typeComboBox, gbc);
        
        // 类别选择
        gbc.gridx = 0;
        gbc.gridy = 3;
        formPanel.add(new JLabel("类别:"), gbc);
        
        gbc.gridx = 1;
        categoryComboBox = new JComboBox<>();
        // updateCategoryComboBox() will be called after typeComboBox is set up
        formPanel.add(categoryComboBox, gbc);

        // AI建议类别 Label
        gbc.gridx = 0;
        gbc.gridy = 4;
        formPanel.add(new JLabel("AI建议类别:"), gbc);

        // AI建议类别 Value Label
        gbc.gridx = 1;
        aiSuggestionValueLabel = new JLabel("(加载中...)");
        formPanel.add(aiSuggestionValueLabel, gbc);

        // 采纳AI建议 Button
        gbc.gridx = 1; // Place it in the second column
        gbc.gridy = 5; // New row for the button
        gbc.anchor = GridBagConstraints.LINE_START; // Align to the left of the cell
        adoptAiButton = new JButton("采纳AI建议");
        adoptAiButton.setEnabled(false); // Initially disabled
        adoptAiButton.addActionListener(e -> {
            if (currentAiSuggestion != null && !currentAiSuggestion.isEmpty()) {
                // Ensure the category list matches the current transaction type
                // This check is implicitly handled if updateCategoryComboBox was called correctly
                // after typeComboBox might have changed.
                categoryComboBox.setSelectedItem(currentAiSuggestion);
            }
        });
        formPanel.add(adoptAiButton, gbc);
        gbc.anchor = GridBagConstraints.CENTER; // Reset anchor for other components

        // 描述输入
        gbc.gridx = 0;
        gbc.gridy = 6; // Adjusted gridy
        formPanel.add(new JLabel("描述:"), gbc);
        
        gbc.gridx = 1;
        descriptionField = new JTextField(20);
        formPanel.add(descriptionField, gbc);
        
        // 支付方式选择
        gbc.gridx = 0;
        gbc.gridy = 7; // Adjusted gridy
        formPanel.add(new JLabel("支付方式:"), gbc);
        
        gbc.gridx = 1;
        paymentMethodComboBox = new JComboBox<>(PAYMENT_METHODS);
        formPanel.add(paymentMethodComboBox, gbc);
        
        mainPanel.add(formPanel, BorderLayout.CENTER);
        
        // 创建按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton cancelButton = new JButton("取消");
        cancelButton.addActionListener(e -> dispose());
        buttonPanel.add(cancelButton);
        
        JButton saveButton = new JButton("保存");
        saveButton.addActionListener(e -> {
            if (saveTransaction()) {
                confirmed = true;
                dispose();
            }
        });
        buttonPanel.add(saveButton);
        
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        setContentPane(mainPanel);
    }
    
    /**
     * 更新日期下拉框选项
     */
    private void updateDayComboBox() {
        int year = (Integer) yearComboBox.getSelectedItem();
        int month = (Integer) monthComboBox.getSelectedItem();
        int daysInMonth = YearMonth.of(year, month).lengthOfMonth();
        
        int selectedDay = -1;
        if (dayComboBox.getItemCount() > 0 && dayComboBox.getSelectedItem() != null) {
            selectedDay = (Integer) dayComboBox.getSelectedItem();
        }
        
        dayComboBox.removeAllItems();
        for (int day = 1; day <= daysInMonth; day++) {
            dayComboBox.addItem(day);
        }
        
        if (selectedDay > 0 && selectedDay <= daysInMonth) {
            dayComboBox.setSelectedItem(selectedDay);
        } else {
            dayComboBox.setSelectedItem(1);
        }
    }
    
    /**
     * 更新类别下拉框选项
     */
    private void updateCategoryComboBox() {
        boolean isExpense = typeComboBox.getSelectedIndex() == 0; // "支出"为第一项
        
        categoryComboBox.removeAllItems();
        
        // 获取对应类型的类别列表
        java.util.List<String> categories;
        if (isExpense) {
            categories = classifier.getExpenseCategories();
        } else {
            categories = classifier.getIncomeCategories();
        }
        
        // 添加到下拉框
        for (String category : categories) {
            categoryComboBox.addItem(category);
        }
        
        // 如果交易记录已存在，尝试选择其当前类别
        if (transaction != null && transaction.getCategory() != null) {
            for (int i = 0; i < categoryComboBox.getItemCount(); i++) {
                if (categoryComboBox.getItemAt(i).equals(transaction.getCategory())) {
                    categoryComboBox.setSelectedIndex(i);
                    break;
                }
            }
        }
    }
    
    /**
     * 加载交易记录数据到表单
     */
    private void loadTransactionData() {
        if (transaction == null) {
            return;
        }
        
        // 设置金额
        amountField.setText(String.format("%.2f", transaction.getAmount()));
        
        // 设置日期
        LocalDate date = transaction.getDate();
        yearComboBox.setSelectedItem(date.getYear());
        monthComboBox.setSelectedItem(date.getMonthValue());
        dayComboBox.setSelectedItem(date.getDayOfMonth());
        
        // 设置类型
        typeComboBox.setSelectedIndex(transaction.isExpense() ? 0 : 1);
        // updateCategoryComboBox is called by typeComboBox's action listener,
        // which will populate categories and select the current one.
        
        // 设置描述
        descriptionField.setText(transaction.getDescription());
        
        // 设置支付方式
        for (int i = 0; i < PAYMENT_METHODS.length; i++) {
            if (PAYMENT_METHODS[i].equals(transaction.getPaymentMethod())) {
                paymentMethodComboBox.setSelectedIndex(i);
                break;
            }
        }
        
        // 类别在updateCategoryComboBox方法中设置

        // 获取并显示AI建议类别
        if (classifier != null && this.transaction != null && this.transaction.getDescription() != null && !this.transaction.getDescription().trim().isEmpty()) {
            try {
                // Ensure the transaction object passed to classifier reflects the current type in the dialog
                // For simplicity, we assume the original transaction type is what AI should classify on,
                // or that the type in dialog hasn't changed yet from original.
                // A more robust solution might re-classify if type is changed in dialog.
                // Corrected constructor call:
                Transaction tempTransactionForAISuggestion = new Transaction(
                    this.transaction.getId(),           // String id
                    this.transaction.getAmount(),       // double amount (can use original or from dialog field)
                    this.transaction.getDate(),         // LocalDate date (can use original or from dialog fields)
                    this.transaction.getCategory(),     // String category (original category is fine for temp object)
                    this.transaction.getDescription(),  // String description (use current description from dialog if it's more up-to-date, or original)
                    typeComboBox.getSelectedIndex() == 0, // boolean isExpense (use current type from dialog)
                    this.transaction.getPaymentMethod() // String paymentMethod (can use original or from dialog field)
                );
                
                // It's better to use the current values from the dialog fields for AI suggestion,
                // as the user might have changed them before wanting an AI suggestion.
                // However, the original transaction's ID and potentially its original category are fine.
                // Let's refine to use dialog values where appropriate for the AI to classify on current input:
                try {
                    double currentAmount = Double.parseDouble(amountField.getText());
                    LocalDate currentDate = LocalDate.of(
                        (Integer) yearComboBox.getSelectedItem(),
                        (Integer) monthComboBox.getSelectedItem(),
                        (Integer) dayComboBox.getSelectedItem()
                    );
                    String currentDescription = descriptionField.getText();
                    String currentPaymentMethod = (String) paymentMethodComboBox.getSelectedItem();

                    tempTransactionForAISuggestion = new Transaction(
                        this.transaction.getId(),           // String id
                        currentAmount,                      // double amount from dialog
                        currentDate,                        // LocalDate date from dialog
                        this.transaction.getCategory(),     // String category (original category is fine for temp object)
                        currentDescription,                 // String description from dialog
                        typeComboBox.getSelectedIndex() == 0, // boolean isExpense (use current type from dialog)
                        currentPaymentMethod                // String paymentMethod from dialog
                    );

                } catch (NumberFormatException nfe) {
                    // If amount is not valid, AI suggestion might not be relevant or possible
                    aiSuggestionValueLabel.setText("(金额格式无效)");
                    adoptAiButton.setEnabled(false);
                    currentAiSuggestion = null; // Ensure no stale suggestion
                    throw nfe; // Re-throw to be caught by the outer catch block or handle specifically
                }


                currentAiSuggestion = classifier.classifyTransaction(tempTransactionForAISuggestion);
                aiSuggestionValueLabel.setText(currentAiSuggestion);
                // Enable button only if suggestion is different from current and valid
                if (currentAiSuggestion != null && !currentAiSuggestion.equals(categoryComboBox.getSelectedItem())) {
                     // Check if currentAiSuggestion is in the categoryComboBox items
                    boolean suggestionIsValid = false;
                    for (int i = 0; i < categoryComboBox.getItemCount(); i++) {
                        if (currentAiSuggestion.equals(categoryComboBox.getItemAt(i))) {
                            suggestionIsValid = true;
                            break;
                        }
                    }
                    adoptAiButton.setEnabled(suggestionIsValid);
                } else {
                    adoptAiButton.setEnabled(false);
                }
            } catch (Exception e) {
                System.err.println("编辑对话框中获取AI分类建议时出错: " + e.getMessage());
                currentAiSuggestion = null;
                aiSuggestionValueLabel.setText("(AI分类出错)");
                adoptAiButton.setEnabled(false);
            }
        } else {
            aiSuggestionValueLabel.setText("(无描述或AI服务不可用)");
            adoptAiButton.setEnabled(false);
        }
    }
    
    /**
     * 保存交易记录
     */
    private boolean saveTransaction() {
        try {
            // 获取输入值
            double amount = Double.parseDouble(amountField.getText());
            int year = (Integer) yearComboBox.getSelectedItem();
            int month = (Integer) monthComboBox.getSelectedItem();
            int day = (Integer) dayComboBox.getSelectedItem();
            LocalDate date = LocalDate.of(year, month, day);
            boolean isExpense = typeComboBox.getSelectedIndex() == 0;
            String category = (String) categoryComboBox.getSelectedItem();
            String description = descriptionField.getText();
            String paymentMethod = (String) paymentMethodComboBox.getSelectedItem();
            
            // 更新交易记录
            transaction.setAmount(amount);
            transaction.setDate(date);
            transaction.setExpense(isExpense);
            
            // 记录原始类别，用于AI学习
            String originalCategory = transaction.getCategory();
            
            transaction.setCategory(category);
            transaction.setDescription(description);
            transaction.setPaymentMethod(paymentMethod);
            
            // 如果类别发生变化，通知分类器学习
            if (!category.equals(originalCategory)) {
                classifier.learnFromUserCorrection(transaction, originalCategory, category);
            }
            
            return true;
        } catch (NumberFormatException e) {
            javax.swing.JOptionPane.showMessageDialog(this, 
                "请输入有效的金额", "输入错误", javax.swing.JOptionPane.ERROR_MESSAGE);
            return false;
        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(this, 
                "保存交易记录时出错: " + e.getMessage(), "错误", javax.swing.JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
    
    /**
     * 获取用户是否确认编辑
     */
    public boolean isConfirmed() {
        return confirmed;
    }
    
    /**
     * 获取编辑后的交易记录
     */
    public Transaction getTransaction() {
        return transaction;
    }
}
