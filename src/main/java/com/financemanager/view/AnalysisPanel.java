package com.financemanager.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;

import com.financemanager.ai.ExpenseAnalyzer;
import com.financemanager.model.BudgetManager; // 需要 BudgetManager 用于建议
import com.financemanager.model.Transaction;
import com.financemanager.model.TransactionManager;

/**
 * 分析报告面板类
 * 包含分析报告选项卡的所有UI和逻辑
 */
public class AnalysisPanel extends JPanel {

    private final TransactionManager transactionManager;
    private final BudgetManager budgetManager; // 添加 BudgetManager 引用
    private final ExpenseAnalyzer analyzer;
    private final MainFrame mainFrame; // 引用主窗口

    private JPanel reportPanel; // 用于显示报告内容的面板
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");


    public AnalysisPanel(TransactionManager transactionManager, BudgetManager budgetManager,
                         ExpenseAnalyzer analyzer, MainFrame mainFrame) {
        this.transactionManager = transactionManager;
        this.budgetManager = budgetManager; // 保存 BudgetManager 引用
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
        reportPanel = new JPanel(); // 初始化 reportPanel
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

        // 添加月度支出趋势分析
        reportPanel.add(new JLabel("<html><h2>月度支出趋势</h2></html>"));
        Map<YearMonth, Double> monthlyTrend = analyzer.analyzeMonthlyTrend(transactions);
        // 这里可以添加图表展示，简化版使用文本展示
        StringBuilder trendText = new StringBuilder("<html><ul>");
        for (Map.Entry<YearMonth, Double> entry : monthlyTrend.entrySet()) {
            trendText.append(String.format("<li>%s: %.2f元</li>", entry.getKey(), entry.getValue()));
        }
        trendText.append("</ul></html>");
        reportPanel.add(new JLabel(trendText.toString()));

        // 添加类别分布分析
        reportPanel.add(new JLabel("<html><h2>支出类别分布</h2></html>"));
        Map<String, Double> categoryDistribution = analyzer.analyzeCategoryDistribution(transactions);
        StringBuilder distributionText = new StringBuilder("<html><ul>");
        for (Map.Entry<String, Double> entry : categoryDistribution.entrySet()) {
            distributionText.append(String.format("<li>%s: %.2f%%</li>", entry.getKey(), entry.getValue()));
        }
        distributionText.append("</ul></html>");
        reportPanel.add(new JLabel(distributionText.toString()));

        // 添加异常支出分析
        reportPanel.add(new JLabel("<html><h2>异常支出检测</h2></html>"));
        List<Transaction> abnormalExpenses = analyzer.detectAbnormalExpenses(transactions);
        if (abnormalExpenses.isEmpty()) {
            reportPanel.add(new JLabel("未检测到异常支出"));
        } else {
            StringBuilder abnormalText = new StringBuilder("<html><ul>");
            for (Transaction t : abnormalExpenses) {
                abnormalText.append(String.format("<li>%s: %.2f元 (%s)</li>",
                        t.getCategory(), t.getAmount(), t.getDate().format(DATE_FORMATTER)));
            }
            abnormalText.append("</ul></html>");
            reportPanel.add(new JLabel(abnormalText.toString()));
        }

        // 添加季节性支出分析
        reportPanel.add(new JLabel("<html><h2>季节性支出模式</h2></html>"));
        Map<Month, List<String>> seasonalPatterns = analyzer.detectSeasonalPatterns(transactions);
        if (seasonalPatterns.isEmpty()) {
            reportPanel.add(new JLabel("未检测到明显的季节性支出模式"));
        } else {
            StringBuilder seasonalText = new StringBuilder("<html><ul>");
            for (Map.Entry<Month, List<String>> entry : seasonalPatterns.entrySet()) {
                seasonalText.append(String.format("<li>%s: %s</li>",
                        entry.getKey(), String.join(", ", entry.getValue())));
            }
            seasonalText.append("</ul></html>");
            reportPanel.add(new JLabel(seasonalText.toString()));
        }

        // 添加预算建议
        reportPanel.add(new JLabel("<html><h2>预算建议</h2></html>"));
        Map<String, Double> budgetSuggestions = analyzer.generateBudgetSuggestions(transactions, budgetManager); // 传递 budgetManager
        if (budgetSuggestions.isEmpty()) {
            reportPanel.add(new JLabel("当前预算设置合理，无需调整"));
        } else {
            StringBuilder suggestionsText = new StringBuilder("<html><ul>");
            for (Map.Entry<String, Double> entry : budgetSuggestions.entrySet()) {
                suggestionsText.append(String.format("<li>%s: 建议预算%.2f元</li>",
                        entry.getKey(), entry.getValue()));
            }
            suggestionsText.append("</ul></html>");
            reportPanel.add(new JLabel(suggestionsText.toString()));
        }

        // 添加节省机会分析
        reportPanel.add(new JLabel("<html><h2>节省机会</h2></html>"));
        List<Map<String, Object>> savingOpportunities = analyzer.analyzeSavingOpportunities(transactions);
        if (savingOpportunities.isEmpty()) {
            reportPanel.add(new JLabel("未发现明显的节省机会"));
        } else {
            StringBuilder opportunitiesText = new StringBuilder("<html><ul>");
            for (Map<String, Object> opportunity : savingOpportunities) {
                opportunitiesText.append(String.format("<li>%s</li>", opportunity.get("description")));
            }
            opportunitiesText.append("</ul></html>");
            reportPanel.add(new JLabel(opportunitiesText.toString()));
        }

        // 刷新UI
        reportPanel.revalidate();
        reportPanel.repaint();
        // 确保包含 reportPanel 的 JScrollPane 也刷新
        this.revalidate();
        this.repaint();
    }
}
