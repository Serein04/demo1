package com.financemanager.view;

import java.awt.BorderLayout;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.JPanel;
import javax.swing.SwingWorker;

import com.financemanager.ai.AIService;
import com.financemanager.ai.ExpenseAnalyzer; // 修改导入
import com.financemanager.model.Transaction;
import com.financemanager.model.TransactionManager;

/**
 * AI助手面板控制器类
 * 协调AIAssistantViewPanel和AIService
 */
public class AIAssistantPanel extends JPanel {
    private final TransactionManager transactionManager;
    private final ExpenseAnalyzer analyzer;
    private final AIService chatService; // 修改类型
    private final AIAssistantViewPanel viewPanel;
    
    private String currentAIResponseId = null; // 用于跟踪流式响应,确保AI回复连续性

    /**
     * 构造函数
     */
    public AIAssistantPanel(TransactionManager transactionManager, ExpenseAnalyzer analyzer) {
        this.transactionManager = transactionManager;
        this.analyzer = analyzer;
        this.chatService = new AIService(); // 修改实例化
        this.viewPanel = new AIAssistantViewPanel();
        
        setLayout(new BorderLayout());
        add(viewPanel, BorderLayout.CENTER);
        
        // 设置视图的回调，将视图的用户操作连接到控制器的方法
        viewPanel.setSendMessageConsumer(this::handleUserMessage);
        viewPanel.setTriggerAnalysisConsumer(this::handleTriggerAnalysis);
        
        // 显示欢迎消息
        viewPanel.displayMessage("AI助手", "您好！我是您的AI财务助手。我可以帮您分析支出情况，提供预算建议，以及回答您关于财务管理的问题。\n\n您可以直接与我聊天，或使用上方的快捷分析功能按钮。");
    }

    /**
     * 处理从视图面板发送过来的用户消息
     * @param message 用户输入的消息
     */
    private void handleUserMessage(String message) {
        viewPanel.displayMessage("您", message); // 在视图上显示用户的消息
        // viewPanel.clearInputField(); // 清空输入框的操作现在由视图自己处理或由控制器决定是否调用
        processUserMessage(message); // 处理用户消息的逻辑
    }

    /**
     * 处理从视图面板触发的快捷分析请求
     * @param analysisPrompt 快捷分析的提示语
     */
    private void handleTriggerAnalysis(String analysisPrompt) {
        viewPanel.displayMessage("您", analysisPrompt); // 在视图上显示用户的请求
        processFinancialQuery(analysisPrompt); // 处理分析请求的逻辑, renamed
    }

    private boolean isFinancialQuery(String message) {
        message = message.toLowerCase().trim();
        // 简单关键词匹配，可以根据需要扩展
        String[] keywords = {
            "分析", "支出", "账单", "消费", "预算", "财务", "钱", "花销",
            "收入", "结余", "报表", "统计", "建议", "情况", "明细",
            "多少", "开销", "费用", "总结", "趋势", "流水", "交易",
            "帮我查", "查一下", "看看我的"
        };
        for (String keyword : keywords) {
            if (message.contains(keyword)) {
                return true;
            }
        }
        // 如果消息长度较长，也可能包含分析意图
        if (message.length() > 15) { // 任意选择一个阈值
             // 避免过长的通用聊天被误判，但也要避免过于简单的判断
            if (message.contains("?") || message.contains("什么") || message.contains("怎么样")) {
                 // 如果是问句，并且包含一些潜在的财务词汇，也认为是财务查询
                for (String keyword : new String[]{"数据", "记录", "账户"}) {
                    if (message.contains(keyword)) return true;
                }
            }
        }
        return false;
    }
    
    /**
     * 处理用户消息的核心逻辑
     */
    private void processUserMessage(String message) {
        // 根据消息内容判断是财务查询还是一般对话
        if (isFinancialQuery(message)) {
            processFinancialQuery(message);
        } else {
            processGeneralQuery(message);
        }
    }

    /**
     * 处理一般对话请求
     */
    private void processGeneralQuery(String message) {
        viewPanel.displayMessage("系统", "AI助手正在思考...");
        viewPanel.setInputEnabled(false);

        final String generalPrompt = String.format(
            "你是一个乐于助人的AI助手。请友好、简洁、直接地回答用户的问题。\n\n" +
            "用户问题：%s",
            message
        );
        executeAiStream(generalPrompt, "ai_general_");
    }
    
    /**
     * 处理财务分析请求的核心逻辑 (原 processAnalysisRequest)
     */
    private void processFinancialQuery(String message) {
        try {
            List<Transaction> transactions = transactionManager.getAllTransactions();
            
            // 对于财务查询，如果无交易数据，则提示用户添加
            if (transactions.isEmpty() && isFinancialQuery(message)) { // 确保只在真实财务查询时检查
                viewPanel.displayMessage("AI助手", "目前还没有任何交易记录。请先添加一些交易记录，我才能为您提供分析和建议。");
                viewPanel.setInputEnabled(true); // Re-enable input if returning early
                return;
            }
            
            viewPanel.displayMessage("系统", "正在分析您的财务数据，请稍候...");
            viewPanel.setInputEnabled(false); // 禁用输入
            
            StringBuilder csvData = new StringBuilder(1024); // Renamed from message to userQuery
            csvData.append("ID,金额,日期,类别,描述,类型,支付方式\n");
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            for (Transaction t : transactions) {
                csvData.append(t.getId()).append(',');
                csvData.append(String.format("%.2f", t.getAmount())).append(',');
                csvData.append(t.getDate().format(dateFormatter)).append(',');
                csvData.append(escapeCSV(t.getCategory())).append(',');
                csvData.append(escapeCSV(t.getDescription())).append(',');
                csvData.append(t.isExpense() ? "支出" : "收入").append(',');
                csvData.append(escapeCSV(t.getPaymentMethod())).append('\n');
            }
            
            String statistics = analyzer.getBasicStatistics(transactions);
            
            final String analysisPromptContent = String.format(
                "你是一个专业的财务分析助手。请基于以下交易数据和统计信息，以专业且友好的口吻回答用户问题：\n\n" +
                "交易数据：\n%s\n\n" +
                "统计信息：\n%s\n\n" +
                "用户问题：%s\n\n" +
                "请提供具体的分析和建议，包括支出趋势、预算建议和财务优化方案。",
                csvData.toString(),
                statistics,
                message // userQuery is 'message' here
            );
            executeAiStream(analysisPromptContent, "ai_financial_");
            
        } catch (Exception e) {
            viewPanel.displayMessage("AI助手", "处理财务分析请求时出现错误：" + e.getMessage());
            viewPanel.setInputEnabled(true); // 确保输入被重新启用
        }
    }

    /**
     * Helper method to execute AI stream call using SwingWorker
     */
    private void executeAiStream(String prompt, String responseIdPrefix) {
        currentAIResponseId = responseIdPrefix + System.currentTimeMillis(); // 标记AI回复开始
        viewPanel.displayMessage("AI助手", ""); // 为流式输出创建占位
        
        new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                chatService.callApiStream(prompt, chunk -> publish(chunk));
                return null;
            }
            
            @Override
            protected void process(List<String> chunks) {
                for (String chunk : chunks) {
                    viewPanel.appendToMessage(chunk); // 追加到视图
                }
            }
            
            @Override
            protected void done() {
                try {
                    get(); // 检查后台任务是否有异常
                } catch (InterruptedException | ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof IOException && cause.getMessage() != null && cause.getMessage().contains("API密钥未配置")) {
                        viewPanel.displayMessage("AI助手", "抱歉，AI助手功能暂时无法使用，因为API密钥未配置。请联系管理员配置API密钥。");
                    } else {
                        viewPanel.displayMessage("AI助手", "抱歉，调用AI服务时出现错误：" + (cause != null ? cause.getMessage() : e.getMessage()));
                    }
                } finally {
                    // Always append newlines if a stream was initiated, to ensure proper separation
                    // before the next user message. displayMessage used in catch already adds its own newlines.
                    if (currentAIResponseId != null) {
                         viewPanel.appendToMessage("\n\n");
                    }
                    currentAIResponseId = null; // 重置
                    viewPanel.setInputEnabled(true); // 重新启用输入
                }
            }
        }.execute();
    }
    
    /**
     * 转义CSV字段中的特殊字符
     */
    private String escapeCSV(String field) {
        if (field == null) return "";
        if (field.contains(",") || field.contains("\n") || field.contains("\"")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }
}
