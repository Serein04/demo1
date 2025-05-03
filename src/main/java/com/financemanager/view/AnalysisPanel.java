package com.financemanager.view;

import java.awt.BorderLayout;
import java.awt.Dimension; // Add Dimension for chart size
import java.awt.FlowLayout;
import java.awt.Font;
import java.text.DecimalFormat; // Add for formatting
import java.text.NumberFormat;  // Add for formatting
import java.time.Month;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap; // Add HashMap for calculations
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;

import com.financemanager.ai.ExpenseAnalyzer;
import com.financemanager.model.BudgetManager;
import com.financemanager.model.Transaction;
import com.financemanager.model.TransactionManager;

/**
 * 分析报告面板类
 * 包含分析报告选项卡的所有UI和逻辑
 */
public class AnalysisPanel extends JPanel {

    private final TransactionManager transactionManager;
    private final BudgetManager budgetManager;
    private final ExpenseAnalyzer analyzer;
    private final MainFrame mainFrame; // 引用主窗口

    private JPanel reportPanel; // 用于显示报告内容的面板
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Font REPORT_FONT = new Font("SimSun", Font.PLAIN, 14); // 增大报告字体
    private static final Font CHART_LABEL_FONT = new Font("SimSun", Font.PLAIN, 12); // 图表标签字体


    public AnalysisPanel(TransactionManager transactionManager, BudgetManager budgetManager,
                         ExpenseAnalyzer analyzer, MainFrame mainFrame) {
        this.transactionManager = transactionManager;
        this.budgetManager = budgetManager;
        this.analyzer = analyzer;
        this.mainFrame = mainFrame;

        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // 添加返回主界面按钮
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton backButton = new JButton("返回主界面");
        backButton.addActionListener(e -> mainFrame.returnToMainScreen());
        topPanel.add(backButton);

        JPanel controlPanel = new JPanel();
        JButton analyzeButton = new JButton("生成分析报告");
        analyzeButton.addActionListener(e -> generateAnalysisReport());
        controlPanel.add(analyzeButton);

        // 组合顶部面板
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(topPanel, BorderLayout.NORTH);
        northPanel.add(controlPanel, BorderLayout.CENTER);

        add(northPanel, BorderLayout.NORTH);

        // 创建报告面板（初始为空）
        reportPanel = new JPanel();
        reportPanel.setLayout(new BoxLayout(reportPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(reportPanel);
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * 生成分析报告
     */
    private void generateAnalysisReport() {
        // 获取所有交易记录
        List<Transaction> transactions = transactionManager.getAllTransactions();

        if (transactions.isEmpty()) {
            JOptionPane.showMessageDialog(this, "没有交易记录可供分析", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // 清空报告面板
        reportPanel.removeAll();
        reportPanel.setFont(REPORT_FONT); // 设置面板默认字体

        // --- 添加月度支出趋势图表 ---
        reportPanel.add(new JLabel("<html><h2>月度支出趋势</h2></html>"));
        Map<YearMonth, Double> monthlyTrend = analyzer.analyzeMonthlyTrend(transactions);
        if (!monthlyTrend.isEmpty()) {
            DefaultPieDataset monthlyDataset = new DefaultPieDataset();
            for (Map.Entry<YearMonth, Double> entry : monthlyTrend.entrySet()) {
                 if (entry.getValue() > 0) {
                    monthlyDataset.setValue(entry.getKey().toString(), entry.getValue()); // 使用 YearMonth 作为 Key
                 }
            }
            JFreeChart monthlyChart = createPieChart(monthlyDataset, "月度支出分布");
            ChartPanel monthlyChartPanel = new ChartPanel(monthlyChart);
            monthlyChartPanel.setPreferredSize(new Dimension(400, 250)); // 设置图表大小
            reportPanel.add(monthlyChartPanel);
        } else {
             JLabel noMonthlyDataLabel = new JLabel("无月度支出数据");
             noMonthlyDataLabel.setFont(REPORT_FONT);
             reportPanel.add(noMonthlyDataLabel);
        }


        // --- 添加类别分布图表 ---
        reportPanel.add(new JLabel("<html><h2>支出类别分布</h2></html>"));
        // 需要计算绝对金额用于图表
        Map<String, Double> categorySpending = calculateCategorySpending(transactions);
        if (!categorySpending.isEmpty()) {
            DefaultPieDataset categoryDataset = new DefaultPieDataset();
            for (Map.Entry<String, Double> entry : categorySpending.entrySet()) {
                 if (entry.getValue() > 0) {
                    categoryDataset.setValue(entry.getKey(), entry.getValue());
                 }
            }
            JFreeChart categoryChart = createPieChart(categoryDataset, "支出类别分布");
            ChartPanel categoryChartPanel = new ChartPanel(categoryChart);
            categoryChartPanel.setPreferredSize(new Dimension(400, 250)); // 设置图表大小
            reportPanel.add(categoryChartPanel);
        } else {
             JLabel noCategoryDataLabel = new JLabel("无类别支出数据");
             noCategoryDataLabel.setFont(REPORT_FONT);
             reportPanel.add(noCategoryDataLabel);
        }


        // --- 添加异常支出分析 ---
        JLabel abnormalTitle = new JLabel("<html><h2>异常支出检测</h2></html>");
        abnormalTitle.setFont(REPORT_FONT);
        reportPanel.add(abnormalTitle);
        List<Transaction> abnormalExpenses = analyzer.detectAbnormalExpenses(transactions);
        if (abnormalExpenses.isEmpty()) {
             JLabel noAbnormalLabel = new JLabel("未检测到异常支出");
             noAbnormalLabel.setFont(REPORT_FONT);
            reportPanel.add(noAbnormalLabel);
        } else {
            StringBuilder abnormalText = new StringBuilder("<html><ul>");
            for (Transaction t : abnormalExpenses) {
                // 使用内联样式增大字体
                abnormalText.append(String.format("<li style='font-size: 11px;'>%s: %.2f元 (%s)</li>",
                        t.getCategory(), t.getAmount(), t.getDate().format(DATE_FORMATTER)));
            }
            abnormalText.append("</ul></html>");
            JLabel abnormalLabel = new JLabel(abnormalText.toString());
            // abnormalLabel.setFont(REPORT_FONT); // HTML 标签会覆盖此设置，所以在 li 中设置
            reportPanel.add(abnormalLabel);
        }

        // --- 添加季节性支出分析 ---
         JLabel seasonalTitle = new JLabel("<html><h2>季节性支出模式</h2></html>");
         seasonalTitle.setFont(REPORT_FONT);
        reportPanel.add(seasonalTitle);
        Map<Month, List<String>> seasonalPatterns = analyzer.detectSeasonalPatterns(transactions);
        if (seasonalPatterns.isEmpty()) {
             JLabel noSeasonalLabel = new JLabel("未检测到明显的季节性支出模式");
             noSeasonalLabel.setFont(REPORT_FONT);
            reportPanel.add(noSeasonalLabel);
        } else {
            StringBuilder seasonalText = new StringBuilder("<html><ul>");
            for (Map.Entry<Month, List<String>> entry : seasonalPatterns.entrySet()) {
                 // 使用内联样式增大字体
                seasonalText.append(String.format("<li style='font-size: 11px;'>%s: %s</li>",
                        entry.getKey(), String.join(", ", entry.getValue())));
            }
            seasonalText.append("</ul></html>");
             JLabel seasonalLabel = new JLabel(seasonalText.toString());
            reportPanel.add(seasonalLabel);
        }

        // --- 添加预算建议 ---
         JLabel suggestionTitle = new JLabel("<html><h2>预算建议</h2></html>");
         suggestionTitle.setFont(REPORT_FONT);
        reportPanel.add(suggestionTitle);
        Map<String, Double> budgetSuggestions = analyzer.generateBudgetSuggestions(transactions, budgetManager);
        if (budgetSuggestions.isEmpty()) {
             JLabel noSuggestionLabel = new JLabel("当前预算设置合理，无需调整");
             noSuggestionLabel.setFont(REPORT_FONT);
            reportPanel.add(noSuggestionLabel);
        } else {
            StringBuilder suggestionsText = new StringBuilder("<html><ul>");
            for (Map.Entry<String, Double> entry : budgetSuggestions.entrySet()) {
                 // 使用内联样式增大字体
                suggestionsText.append(String.format("<li style='font-size: 11px;'>%s: 建议预算%.2f元</li>",
                        entry.getKey(), entry.getValue()));
            }
            suggestionsText.append("</ul></html>");
             JLabel suggestionLabel = new JLabel(suggestionsText.toString());
            reportPanel.add(suggestionLabel);
        }

        // --- 添加节省机会分析 ---
         JLabel savingTitle = new JLabel("<html><h2>节省机会</h2></html>");
         savingTitle.setFont(REPORT_FONT);
        reportPanel.add(savingTitle);
        List<Map<String, Object>> savingOpportunities = analyzer.analyzeSavingOpportunities(transactions);
        if (savingOpportunities.isEmpty()) {
             JLabel noSavingLabel = new JLabel("未发现明显的节省机会");
             noSavingLabel.setFont(REPORT_FONT);
            reportPanel.add(noSavingLabel);
        } else {
            StringBuilder opportunitiesText = new StringBuilder("<html><ul>");
            for (Map<String, Object> opportunity : savingOpportunities) {
                 // 使用内联样式增大字体
                opportunitiesText.append(String.format("<li style='font-size: 11px;'>%s</li>", opportunity.get("description")));
            }
            opportunitiesText.append("</ul></html>");
             JLabel savingLabel = new JLabel(opportunitiesText.toString());
            reportPanel.add(savingLabel);
        }

        // 刷新UI
        reportPanel.revalidate();
        reportPanel.repaint();
        // 确保包含 reportPanel 的 JScrollPane 也刷新
        this.revalidate();
        this.repaint();
    }

    /**
     * 创建一个配置好的饼图
     * @param dataset 数据集
     * @param title 图表标题 (可选)
     * @return JFreeChart 对象
     */
    private JFreeChart createPieChart(PieDataset dataset, String title) {
        JFreeChart chart = ChartFactory.createPieChart(
                null, // 不在图表内部显示主标题
                dataset,
                false, // 不显示图例
                true,  // 显示工具提示
                false  // 不生成URL
        );

        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setLabelFont(CHART_LABEL_FONT); // 使用统一的标签字体

        // 设置标签格式: "类别: ¥金额 (百分比)"
        String labelFormat = "{0}: {1} ({2})";
        StandardPieSectionLabelGenerator labelGenerator = new StandardPieSectionLabelGenerator(
            labelFormat,
            NumberFormat.getCurrencyInstance(Locale.CHINA),
            new DecimalFormat("0.0%")
        );
        plot.setLabelGenerator(labelGenerator);

        plot.setNoDataMessage("无可用数据");
        plot.setNoDataMessageFont(CHART_LABEL_FONT);

        // 可以设置背景透明等美化选项
        plot.setBackgroundPaint(null);
        plot.setOutlineVisible(false);

        return chart;
    }

     /**
     * 计算所有交易中各支出类别的总金额
     * @param transactions 交易列表
     * @return 各类别及其总支出金额的 Map
     */
    private Map<String, Double> calculateCategorySpending(List<Transaction> transactions) {
        Map<String, Double> categorySpending = new HashMap<>();
        for (Transaction t : transactions) {
            if (t.isExpense()) {
                String category = t.getCategory();
                categorySpending.put(category,
                        categorySpending.getOrDefault(category, 0.0) + t.getAmount());
            }
        }
        return categorySpending;
    }
}
