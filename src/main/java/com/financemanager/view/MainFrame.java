package com.financemanager.view;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;

import com.financemanager.ai.ExpenseAnalyzer;
import com.financemanager.ai.TransactionClassifier;
import com.financemanager.model.BudgetManager;
import com.financemanager.model.TransactionManager;

// 移除 JFreeChart 相关导入

/**
 * 主窗口类 (重构后)
 * 负责组装各个功能面板到选项卡中
 */
public class MainFrame extends JFrame {
    private final TransactionManager transactionManager;
    private final BudgetManager budgetManager;
    private final TransactionClassifier classifier;
    private final ExpenseAnalyzer analyzer;
    // private StartFrame startFrame; // Removed, StartFrame is deleted

    // UI组件
    private JTabbedPane tabbedPane;
    // private BudgetPanel budgetPanel; // Removed, BudgetPanel is deleted

    // 移除 TransactionPanel, BudgetPanel, AnalysisPanel 的 UI 组件声明

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
        // loadTransactions() 和 loadBudgetData() 的调用移到各自 Panel 的构造函数中
    }

    /**
     * 设置StartFrame引用
     */
    // public void setStartFrame(StartFrame startFrame) { // Removed, StartFrame is deleted
    // this.startFrame = startFrame;
    // }

    /**
     * 初始化UI组件
     */
    private void initUI() {
        setTitle("个人财务管理器 (Legacy Swing Frame)"); // Indicate it's legacy
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // Dispose instead of exit if it's just for remaining panels
        setSize(900, 600);
        setLocationRelativeTo(null);

        // 创建选项卡面板
        tabbedPane = new JTabbedPane();

        // // 创建并添加交易记录选项卡 - Removed, TransactionPanel is deleted
        // TransactionPanel transactionPanel = new TransactionPanel(transactionManager, classifier, this);
        // tabbedPane.addTab("交易记录", transactionPanel);

        // // 创建并添加预算管理选项卡 - Removed, BudgetPanel is deleted
        // budgetPanel = new BudgetPanel(transactionManager, budgetManager, classifier, this); 
        // tabbedPane.addTab("预算管理", budgetPanel);

        // // 创建并添加分析报告选项卡 - Removed, AnalysisPanel is deleted
        // AnalysisPanel analysisPanel = new AnalysisPanel(transactionManager, budgetManager, analyzer, this);
        // tabbedPane.addTab("分析报告", analysisPanel);

        // 创建并添加AI助手选项卡 (AIAssistantPanel 本身已是独立类)
        // This is the only remaining panel for now.
        AIAssistantPanel aiAssistantPanel = new AIAssistantPanel(transactionManager, analyzer, this); 
        tabbedPane.addTab("AI助手", aiAssistantPanel);

        // 设置内容面板
        setContentPane(tabbedPane);
    }

    /**
     * 返回主界面 (改为 public)
     */
    // public void returnToMainScreen() { // Removed, StartFrame is deleted and navigation is JavaFX based
    // if (startFrame != null) {
    // this.dispose(); // 关闭当前窗口
    // startFrame.setVisible(true); // 显示StartFrame
    // } else {
    // System.out.println("StartFrame is null, disposing MainFrame."); 
    // this.dispose();
    // }
    // }

    /**
     * 刷新预算面板的数据（包括表格和图表）
     * 供 TransactionPanel 调用
     */
    // public void refreshBudgetPanel() { // Removed, BudgetPanel is deleted
    // if (budgetPanel != null) {
    // budgetPanel.refreshData(); 
    // }
    // }

    // 移除 createTransactionPanel(), createBudgetPanel(), createAnalysisPanel() 方法
    // 移除 loadTransactions(), loadBudgetData(), updateBudgetTable(), updateBudgetChart(), createCategoryPieChartDataset() 方法
    // 移除 addTransaction(), deleteTransaction(), editTransaction(), importFromCSV() 方法
    // 移除 setMonthlyBudget(), setSavingsGoal(), setCategoryBudget() 方法
    // 移除 generateAnalysisReport() 方法
    // 移除 updateCategoryComboBox() 方法 (已移至 TransactionPanel)
    // 移除 updateDaysInMonth() 方法 (已移至 TransactionPanel)

    // getTabbedPane() 方法保留，如果其他地方需要访问
    public JTabbedPane getTabbedPane() {
        return tabbedPane;
    }
}
