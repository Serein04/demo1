package com.financemanager.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;

import com.financemanager.ai.TransactionClassifier; // 需要 classifier 获取类别
import com.financemanager.model.BudgetManager;
import com.financemanager.model.Transaction;
import com.financemanager.model.TransactionManager;

/**
 * 预算管理面板类
 * 包含预算管理选项卡的所有UI和逻辑
 */
public class BudgetPanel extends JPanel {

    private final TransactionManager transactionManager;
    private final BudgetManager budgetManager;
    private final TransactionClassifier classifier; // 添加 classifier 引用
    private final MainFrame mainFrame; // 引用主窗口

    // 预算设置组件
    private JTextField monthlyBudgetField;
    private JTextField savingsGoalField;
    private JComboBox<String> budgetCategoryComboBox;
    private JTextField categoryBudgetField;
    private JButton setBudgetButton;
    private JTable budgetTable;
    private DefaultTableModel budgetTableModel;
    private JPanel budgetChartPanel;

    public BudgetPanel(TransactionManager transactionManager, BudgetManager budgetManager,
                       TransactionClassifier classifier, MainFrame mainFrame) {
        this.transactionManager = transactionManager;
        this.budgetManager = budgetManager;
        this.classifier = classifier; // 保存 classifier 引用
        this.mainFrame = mainFrame;

        initComponents();
        loadBudgetData(); // 初始化时加载数据
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // 添加返回主界面按钮
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton backButton = new JButton("返回主界面");
        backButton.addActionListener(e -> mainFrame.returnToMainScreen());
        topPanel.add(backButton);
        add(topPanel, BorderLayout.NORTH);

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
        // 使用 classifier 获取支出类别
        if (classifier != null) {
             for (String category : classifier.getExpenseCategories()) {
                budgetCategoryComboBox.addItem(category);
            }
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
        budgetTableModel = new DefaultTableModel(columnNames, 0) {
             @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 表格不可编辑
            }
        };

        // 创建表格
        budgetTable = new JTable(budgetTableModel);
        JScrollPane scrollPane = new JScrollPane(budgetTable);
        budgetDisplayPanel.add(scrollPane, BorderLayout.CENTER);

        // 创建图表面板
        budgetChartPanel = new JPanel(new BorderLayout());
        budgetChartPanel.setBorder(BorderFactory.createTitledBorder("当月支出分布图"));
        budgetChartPanel.setPreferredSize(new Dimension(400, 250)); // 设置一个初始大小

        // 添加面板
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(settingsPanel, BorderLayout.NORTH);
        centerPanel.add(budgetDisplayPanel, BorderLayout.CENTER);
        centerPanel.add(budgetChartPanel, BorderLayout.SOUTH); // 将图表面板添加到下方
        add(centerPanel, BorderLayout.CENTER);
    }

     /**
     * 加载预算数据并更新UI
     */
    public void loadBudgetData() { // 改为 public
        // 显示月度预算和储蓄目标
        monthlyBudgetField.setText(String.format("%.2f", budgetManager.getMonthlyBudget()));
        savingsGoalField.setText(String.format("%.2f", budgetManager.getSavingsGoal()));

        // 显示当前选中类别的预算
        String selectedCategory = (String) budgetCategoryComboBox.getSelectedItem();
        if (selectedCategory != null) {
            double budget = budgetManager.getCategoryBudget(selectedCategory);
            categoryBudgetField.setText(String.format("%.2f", budget));
        } else if (budgetCategoryComboBox.getItemCount() > 0) {
            // 如果没有选中项，默认显示第一个类别的预算
            budgetCategoryComboBox.setSelectedIndex(0);
            selectedCategory = (String) budgetCategoryComboBox.getSelectedItem();
             double budget = budgetManager.getCategoryBudget(selectedCategory);
            categoryBudgetField.setText(String.format("%.2f", budget));
        }


        // 更新预算表格和图表
        updateBudgetTable();
        updateBudgetChart();
    }

    /**
     * 创建当月支出类别饼图的数据集
     */
    private PieDataset createCategoryPieChartDataset() {
        DefaultPieDataset dataset = new DefaultPieDataset();
        YearMonth currentMonth = YearMonth.now();
        Map<String, Double> categorySpending = new HashMap<>();

        // 从 TransactionManager 获取当月支出数据
        List<Transaction> transactions = transactionManager.getAllTransactions();
        for (Transaction t : transactions) {
            if (t.isExpense() && YearMonth.from(t.getDate()).equals(currentMonth)) {
                String category = t.getCategory();
                categorySpending.put(category,
                        categorySpending.getOrDefault(category, 0.0) + t.getAmount());
            }
        }

        // 将数据添加到 PieDataset
        for (Map.Entry<String, Double> entry : categorySpending.entrySet()) {
            // 忽略金额为0或负数的类别
            if (entry.getValue() > 0) {
                dataset.setValue(entry.getKey(), entry.getValue());
            }
        }

        return dataset;
    }

    /**
     * 更新预算图表
     */
    private void updateBudgetChart() {
        PieDataset dataset = createCategoryPieChartDataset();

        // 创建饼图
        JFreeChart chart = ChartFactory.createPieChart(
                null,  // 图表标题，已在面板边框设置
                dataset,        // 数据集
                false,          // 不显示图例
                true,           // 生成工具提示
                false           // 不生成URL
        );

        // 设置中文字体，防止乱码
        Font font = new Font("SimSun", Font.PLAIN, 12); // 使用宋体
        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setLabelFont(font);

        // --- 自定义标签格式 ---
        String labelFormat = "{0}: {1} ({2})"; // 格式：类别: 金额 (百分比)
        StandardPieSectionLabelGenerator labelGenerator = new StandardPieSectionLabelGenerator(
            labelFormat,
            NumberFormat.getCurrencyInstance(Locale.CHINA), // 金额使用中国货币格式
            new DecimalFormat("0.0%")  // 百分比保留一位小数
        );
        plot.setLabelGenerator(labelGenerator);
        // --- 结束自定义 ---

        plot.setNoDataMessage("当前月份无支出数据");
        plot.setNoDataMessageFont(font);

        // 创建 ChartPanel
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setMouseWheelEnabled(true); // 允许鼠标滚轮缩放

        // 更新 budgetChartPanel 内容
        budgetChartPanel.removeAll(); // 移除旧图表
        budgetChartPanel.add(chartPanel, BorderLayout.CENTER); // 添加新图表
        budgetChartPanel.revalidate(); // 重新验证布局
        budgetChartPanel.repaint();    // 重绘面板
    }

    /**
     * 更新预算表格
     */
    private void updateBudgetTable() {
        // 清空表格
        budgetTableModel.setRowCount(0);

        // 获取所有交易记录
        List<Transaction> transactions = transactionManager.getAllTransactions();

        // 获取当前月份
        YearMonth currentMonth = YearMonth.now();

        // 计算当前月份的支出
        Map<String, Double> categorySpending = new HashMap<>();
        for (Transaction t : transactions) {
            if (t.isExpense() && YearMonth.from(t.getDate()).equals(currentMonth)) {
                String category = t.getCategory();
                categorySpending.put(category,
                        categorySpending.getOrDefault(category, 0.0) + t.getAmount());
            }
        }

        // 获取所有类别预算
        Map<String, Double> categoryBudgets = budgetManager.getAllCategoryBudgets();

        // 添加月度总预算行
        double totalSpending = categorySpending.values().stream().mapToDouble(Double::doubleValue).sum();
        double monthlyBudget = budgetManager.getMonthlyBudget();
        double remainingBudget = monthlyBudget - totalSpending;
        String status = remainingBudget >= 0 ? "正常" : "超支";

        Object[] totalRow = {
                "总计",
                String.format("%.2f", monthlyBudget),
                String.format("%.2f", totalSpending),
                String.format("%.2f", remainingBudget),
                status
        };
        budgetTableModel.addRow(totalRow);

        // 添加各类别预算行
        for (Map.Entry<String, Double> entry : categoryBudgets.entrySet()) {
            String category = entry.getKey();
            double budget = entry.getValue();
            double spending = categorySpending.getOrDefault(category, 0.0);
            double remaining = budget - spending;
            status = remaining >= 0 ? "正常" : "超支";

            Object[] row = {
                    category,
                    String.format("%.2f", budget),
                    String.format("%.2f", spending),
                    String.format("%.2f", remaining),
                    status
            };
            budgetTableModel.addRow(row);
        }

        // 添加未设置预算但有支出的类别
        for (String category : categorySpending.keySet()) {
            if (!categoryBudgets.containsKey(category)) {
                double spending = categorySpending.get(category);

                Object[] row = {
                        category,
                        "未设置",
                        String.format("%.2f", spending),
                        "N/A",
                        "未设置预算"
                };
                budgetTableModel.addRow(row);
            }
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
            // 更新预算表格和图表
            updateBudgetTable();
            updateBudgetChart();
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
            // 储蓄目标变化通常不直接影响当前支出图表，但如果需要可以更新
            // updateBudgetChart();
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
                // 更新预算表格和图表
                updateBudgetTable();
                updateBudgetChart();
            } else {
                 JOptionPane.showMessageDialog(this, "请先选择一个类别", "提示", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "请输入有效的金额", "输入错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 供 MainFrame 调用的刷新方法
     */
    public void refreshData() {
        loadBudgetData();
    }
}
