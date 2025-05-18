package com.financemanager.view;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.financemanager.ai.AIService;
import com.financemanager.ai.ExpenseAnalyzer;
import com.financemanager.ai.TransactionClassifier;
import com.financemanager.model.BudgetManager;
import com.financemanager.model.Transaction;
import com.financemanager.model.TransactionManager;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

public class AIAssistantViewController {

    @FXML
    private Button backButton;
    @FXML
    private ScrollPane chatScrollPane;
    @FXML
    private TextFlow chatAreaTextFlow;
    @FXML
    private Button spendingAnalysisButton;
    @FXML
    private Button budgetSuggestionButton;
    @FXML
    private Button savingOpportunitiesButton;
    @FXML
    private Button seasonalPatternsButton;
    @FXML
    private TextField inputField;
    @FXML
    private Button sendButton;

    private TransactionManager transactionManager;
    private BudgetManager budgetManager;
    private TransactionClassifier transactionClassifier;
    private ExpenseAnalyzer expenseAnalyzer;
    private AIService aiService;

    private Text currentAiResponseTextNode; // For streaming to a single Text node

    @FXML
    public void initialize() {
        // Ensure chatScrollPane follows new messages
        chatAreaTextFlow.heightProperty().addListener((observable, oldValue, newValue) -> {
            chatScrollPane.setVvalue(1.0); // Scroll to bottom
        });
        displayWelcomeMessage();
        System.out.println("AIAssistantViewController initialized.");
    }

    public void setServices(TransactionManager tm, BudgetManager bm, TransactionClassifier tc, ExpenseAnalyzer ea, AIService as) {
        this.transactionManager = tm;
        this.budgetManager = bm;
        this.transactionClassifier = tc;
        this.expenseAnalyzer = ea;
        this.aiService = as; // AIService is now injected
    }

    private void displayWelcomeMessage() {
        displayMessage("AI助手", "您好！我是您的AI财务助手。我可以帮您分析支出情况，提供预算建议，以及回答您关于财务管理的问题。\n\n您可以直接与我聊天，或使用下方的快捷分析功能按钮。", Color.BLUE);
    }
    
    private void displayMessage(String sender, String message, Color textColor) {
        Text senderText = new Text(String.format("[%s]: ", sender));
        senderText.setFont(Font.font("System", FontWeight.BOLD, 14));
        senderText.setFill(textColor);

        Text messageText = new Text(message + "\n\n");
        messageText.setFont(Font.font("System", FontWeight.NORMAL, 14));
        messageText.setFill(Color.BLACK);
        
        Platform.runLater(() -> {
            chatAreaTextFlow.getChildren().addAll(senderText, messageText);
        });
    }

    // For streaming AI responses
    private void prepareForStreamingResponse(String sender, Color textColor) {
        Text senderText = new Text(String.format("[%s]: ", sender));
        senderText.setFont(Font.font("System", FontWeight.BOLD, 14));
        senderText.setFill(textColor);

        currentAiResponseTextNode = new Text(); // Create new Text node for the AI's message
        currentAiResponseTextNode.setFont(Font.font("System", FontWeight.NORMAL, 14));
        currentAiResponseTextNode.setFill(Color.BLACK);
        
        Platform.runLater(() -> {
            chatAreaTextFlow.getChildren().addAll(senderText, currentAiResponseTextNode, new Text("\n\n"));
        });
    }
    
    private void appendToStreamingResponse(String chunk) {
        String cleanedChunk = chunk.replace("*", ""); // Basic cleaning
        Platform.runLater(() -> {
            if (currentAiResponseTextNode != null) {
                currentAiResponseTextNode.setText(currentAiResponseTextNode.getText() + cleanedChunk);
            }
        });
    }


    @FXML
    private void handleSendMessage(ActionEvent event) {
        String message = inputField.getText().trim();
        if (!message.isEmpty()) {
            displayMessage("您", message, Color.DARKGREEN);
            inputField.clear();
            processUserMessage(message);
        }
    }

    @FXML
    private void handleSpendingAnalysis(ActionEvent event) {
        String prompt = "请分析我的支出情况，找出主要支出类别和趋势。";
        displayMessage("您", prompt, Color.DARKGREEN);
        processFinancialQuery(prompt);
    }

    @FXML
    private void handleBudgetSuggestion(ActionEvent event) {
        String prompt = "根据我的消费习惯，请给我提供合理的预算建议。";
        displayMessage("您", prompt, Color.DARKGREEN);
        processFinancialQuery(prompt);
    }

    @FXML
    private void handleSavingOpportunities(ActionEvent event) {
        String prompt = "请分析我的支出，找出可能的节省机会。";
        displayMessage("您", prompt, Color.DARKGREEN);
        processFinancialQuery(prompt);
    }

    @FXML
    private void handleSeasonalPatterns(ActionEvent event) {
        String prompt = "请分析我的消费是否存在季节性模式。";
        displayMessage("您", prompt, Color.DARKGREEN);
        processFinancialQuery(prompt);
    }
    
    private void processUserMessage(String message) {
        if (isFinancialQuery(message)) {
            processFinancialQuery(message);
        } else {
            processGeneralQuery(message);
        }
    }

    private boolean isFinancialQuery(String message) { // Same as in AIAssistantPanel
        message = message.toLowerCase().trim();
        String[] keywords = {"分析", "支出", "账单", "消费", "预算", "财务", "钱", "花销", "收入", "结余", "报表", "统计", "建议", "情况", "明细", "多少", "开销", "费用", "总结", "趋势", "流水", "交易", "帮我查", "查一下", "看看我的"};
        for (String keyword : keywords) {
            if (message.contains(keyword)) return true;
        }
        if (message.length() > 15) { 
            if (message.contains("?") || message.contains("什么") || message.contains("怎么样")) {
                for (String keyword : new String[]{"数据", "记录", "账户"}) {
                    if (message.contains(keyword)) return true;
                }
            }
        }
        return false;
    }

    private void processGeneralQuery(String message) {
        setInputEnabled(false);
        prepareForStreamingResponse("AI助手", Color.BLUE);

        final String generalPrompt = String.format("你是一个乐于助人的AI助手。请友好、简洁、直接地回答用户的问题。\n\n用户问题：%s", message);
        executeAiStreamTask(generalPrompt);
    }

    private void processFinancialQuery(String userQuery) {
        if (transactionManager == null || expenseAnalyzer == null || aiService == null) {
            showAlert("错误", "核心服务未初始化，无法处理财务查询。");
            return;
        }
        try {
            List<Transaction> transactions = transactionManager.getAllTransactions();
            if (transactions.isEmpty() && isFinancialQuery(userQuery)) {
                displayMessage("AI助手", "目前还没有任何交易记录。请先添加一些交易记录，我才能为您提供分析和建议。", Color.ORANGERED);
                setInputEnabled(true);
                return;
            }
            
            setInputEnabled(false);
            prepareForStreamingResponse("AI助手", Color.BLUE);

            StringBuilder csvData = new StringBuilder("ID,金额,日期,类别,描述,类型,支付方式\n");
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            for (Transaction t : transactions) {
                csvData.append(t.getId()).append(',').append(String.format("%.2f", t.getAmount())).append(',')
                       .append(t.getDate().format(dateFormatter)).append(',').append(escapeCSV(t.getCategory())).append(',')
                       .append(escapeCSV(t.getDescription())).append(',').append(t.isExpense() ? "支出" : "收入").append(',')
                       .append(escapeCSV(t.getPaymentMethod())).append('\n');
            }
            String statistics = expenseAnalyzer.getBasicStatistics(transactions);
            final String analysisPromptContent = String.format(
                "你是一个专业的财务分析助手。请基于以下交易数据和统计信息，以专业且友好的口吻回答用户问题：\n\n交易数据：\n%s\n\n统计信息：\n%s\n\n用户问题：%s\n\n请提供具体的分析和建议，包括支出趋势、预算建议和财务优化方案。",
                csvData.toString(), statistics, userQuery);
            
            executeAiStreamTask(analysisPromptContent);

        } catch (Exception e) {
            displayMessage("AI助手", "处理财务分析请求时出现错误：" + e.getMessage(), Color.RED);
            setInputEnabled(true);
            e.printStackTrace();
        }
    }
    
    private void executeAiStreamTask(String prompt) {
        Task<Void> aiTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                aiService.callApiStream(prompt, chunk -> Platform.runLater(() -> appendToStreamingResponse(chunk)));
                return null;
            }

            @Override
            protected void succeeded() {
                super.succeeded();
                setInputEnabled(true);
            }

            @Override
            protected void failed() {
                super.failed();
                Throwable e = getException();
                String errorMessage = "抱歉，调用AI服务时出现错误：" + (e != null ? e.getMessage() : "未知错误");
                if (e instanceof IOException && e.getMessage() != null && e.getMessage().contains("API密钥未配置")) {
                    errorMessage = "抱歉，AI助手功能暂时无法使用，因为API密钥未配置。请联系管理员配置API密钥。";
                }
                displayMessage("AI助手", errorMessage, Color.RED);
                setInputEnabled(true);
                if (e != null) e.printStackTrace();
            }
        };
        new Thread(aiTask).start();
    }

    private String escapeCSV(String field) {
        if (field == null) return "";
        if (field.contains(",") || field.contains("\n") || field.contains("\"")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }

    private void setInputEnabled(boolean enabled) {
        inputField.setDisable(!enabled);
        sendButton.setDisable(!enabled);
        spendingAnalysisButton.setDisable(!enabled);
        budgetSuggestionButton.setDisable(!enabled);
        savingOpportunitiesButton.setDisable(!enabled);
        seasonalPatternsButton.setDisable(!enabled);
    }

    @FXML
    private void handleBackButtonAction(ActionEvent event) {
        try {
            Stage stage = (Stage) backButton.getScene().getWindow();
            if (stage.getScene() != null && stage.getScene().getRoot() != null) {
                stage.getScene().setRoot(new javafx.scene.layout.Pane());
            }
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/financemanager/view/StartScreen.fxml"));
            Parent startScreenRoot = loader.load();
            StartScreenController ssc = loader.getController();
            ssc.setServices(transactionManager, budgetManager, transactionClassifier, expenseAnalyzer, aiService);
            stage.setScene(new Scene(startScreenRoot, 1200, 800));
            stage.setTitle("个人财务管理器 - JavaFX");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("错误", "无法加载主菜单界面: " + e.getMessage());
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
