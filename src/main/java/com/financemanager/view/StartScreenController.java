package com.financemanager.view;

import java.io.IOException;

import com.financemanager.ai.AIService; // Import AIService
import com.financemanager.ai.ExpenseAnalyzer;
import com.financemanager.ai.TransactionClassifier;
import com.financemanager.model.BudgetManager;
import com.financemanager.model.TransactionManager;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public class StartScreenController {

    @FXML
    private Button transactionButton;

    @FXML
    private Button budgetButton;

    @FXML
    private Button analysisButton;

    @FXML
    private Button aiButton;

    private TransactionManager transactionManager;
    private BudgetManager budgetManager;
    private TransactionClassifier transactionClassifier;
    private ExpenseAnalyzer expenseAnalyzer;
    private AIService aiService; // Add AIService field

    // Method to receive services from MainJavaFX
    public void setServices(TransactionManager transactionManager, BudgetManager budgetManager, 
                            TransactionClassifier transactionClassifier, ExpenseAnalyzer expenseAnalyzer, AIService aiService) {
        this.transactionManager = transactionManager;
        this.budgetManager = budgetManager;
        this.transactionClassifier = transactionClassifier;
        this.expenseAnalyzer = expenseAnalyzer;
        this.aiService = aiService; // Store AIService
        System.out.println("Services (TM, BM, TC, EA, AS) set in StartScreenController.");
    }

    // Initialize method (called after FXML loading)
    @FXML
    public void initialize() {
        // You can set additional properties or listeners here if needed
        System.out.println("StartScreenController initialized.");
    }

    @FXML
    private void handleTransactionButtonAction(ActionEvent event) {
        System.out.println("Transaction Button Clicked!");
        if (transactionManager == null || transactionClassifier == null) {
            System.err.println("Services not set in StartScreenController. Cannot navigate to TransactionView.");
            // Optionally show an alert to the user
            return;
        }
        try {
            Stage stage = (Stage) transactionButton.getScene().getWindow();
            // Attempt to detach old root explicitly from the current scene of the stage
            if (stage.getScene() != null && stage.getScene().getRoot() != null) {
                stage.getScene().setRoot(new javafx.scene.layout.Pane()); // Set a dummy, new, empty root
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/financemanager/view/TransactionView.fxml"));
            Parent transactionViewRoot = loader.load();

            TransactionViewController controller = loader.getController();
            // Pass all five services to TransactionViewController
            controller.setServices(transactionManager, budgetManager, transactionClassifier, expenseAnalyzer, aiService); 

            Scene newScene = new Scene(transactionViewRoot, 1200, 800); // Updated size
            stage.setScene(newScene);
            stage.setTitle("交易记录管理 - JavaFX");

        } catch (IOException e) {
            e.printStackTrace();
            // Show error alert
             new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "无法加载交易记录界面: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void handleBudgetButtonAction(ActionEvent event) {
        System.out.println("Budget Button Clicked!");
        if (transactionManager == null || budgetManager == null || transactionClassifier == null) {
            System.err.println("Services not fully set in StartScreenController. Cannot navigate to BudgetView.");
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "核心服务未初始化，无法打开预算管理。").showAndWait();
            return;
        }
        try {
            Stage stage = (Stage) budgetButton.getScene().getWindow();
            if (stage.getScene() != null && stage.getScene().getRoot() != null) {
                stage.getScene().setRoot(new javafx.scene.layout.Pane()); 
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/financemanager/view/BudgetView.fxml"));
            Parent budgetViewRoot = loader.load();

            BudgetViewController controller = loader.getController();
            // Pass ExpenseAnalyzer and AIService as well to BudgetViewController
            controller.setServices(transactionManager, budgetManager, transactionClassifier, expenseAnalyzer, aiService);

            Scene newScene = new Scene(budgetViewRoot, 1200, 800); // Updated size
            stage.setScene(newScene);
            stage.setTitle("预算管理 - JavaFX");

        } catch (IOException e) {
            e.printStackTrace();
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "无法加载预算管理界面: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void handleAnalysisButtonAction(ActionEvent event) {
        System.out.println("Analysis Button Clicked!");
        if (transactionManager == null || budgetManager == null || transactionClassifier == null || expenseAnalyzer == null) {
            System.err.println("Services not fully set in StartScreenController. Cannot navigate to AnalysisView.");
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "核心服务未初始化，无法打开分析报告。").showAndWait();
            return;
        }
        try {
            Stage stage = (Stage) analysisButton.getScene().getWindow();
            if (stage.getScene() != null && stage.getScene().getRoot() != null) {
                stage.getScene().setRoot(new javafx.scene.layout.Pane());
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/financemanager/view/AnalysisView.fxml"));
            Parent analysisViewRoot = loader.load();

            AnalysisViewController controller = loader.getController();
            controller.setServices(transactionManager, budgetManager, transactionClassifier, expenseAnalyzer, aiService);

            Scene newScene = new Scene(analysisViewRoot, 1200, 800); // Updated size
            stage.setScene(newScene);
            stage.setTitle("分析报告 - JavaFX");

        } catch (IOException e) {
            e.printStackTrace();
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "无法加载分析报告界面: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void handleAiButtonAction(ActionEvent event) {
        System.out.println("AI Assistant Button Clicked!");
        if (transactionManager == null || budgetManager == null || transactionClassifier == null || expenseAnalyzer == null || aiService == null) {
            System.err.println("Services not fully set in StartScreenController. Cannot navigate to AIAssistantView.");
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "核心服务未初始化，无法打开AI助手。").showAndWait();
            return;
        }
        try {
            Stage stage = (Stage) aiButton.getScene().getWindow();
            if (stage.getScene() != null && stage.getScene().getRoot() != null) {
                stage.getScene().setRoot(new javafx.scene.layout.Pane());
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/financemanager/view/AIAssistantView.fxml"));
            Parent aiAssistantViewRoot = loader.load();

            AIAssistantViewController controller = loader.getController();
            controller.setServices(transactionManager, budgetManager, transactionClassifier, expenseAnalyzer, aiService);

            Scene newScene = new Scene(aiAssistantViewRoot, 1200, 800);
            stage.setScene(newScene);
            stage.setTitle("AI助手 - JavaFX");

        } catch (IOException e) {
            e.printStackTrace();
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "无法加载AI助手界面: " + e.getMessage()).showAndWait();
        }
    }

    // Placeholder for navigation logic
    // This will be more complex, involving loading new FXMLs or switching scenes
    // For now, we'll just print to console.
}
