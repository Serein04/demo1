package com.financemanager;

import java.io.IOException;
import java.net.URL;

import com.financemanager.ai.AIService;
import com.financemanager.ai.ExpenseAnalyzer;
import com.financemanager.ai.TransactionClassifier;
import com.financemanager.model.BudgetManager;
import com.financemanager.model.TransactionManager;
import com.financemanager.view.StartScreenController;

import javafx.application.Application; // Import ExpenseAnalyzer
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainJavaFX extends Application {

    private TransactionManager transactionManager;
    private BudgetManager budgetManager;
    private TransactionClassifier transactionClassifier;
    private ExpenseAnalyzer expenseAnalyzer; // Add ExpenseAnalyzer
    private AIService aiService;

    @Override
    public void init() throws Exception {
        super.init();
        // Initialize services here, so they are ready before start()
        this.aiService = new AIService();
        this.transactionManager = new TransactionManager();
        this.budgetManager = new BudgetManager();
        this.transactionClassifier = new TransactionClassifier(this.aiService);
        this.expenseAnalyzer = new ExpenseAnalyzer(this.transactionClassifier); // Initialize ExpenseAnalyzer
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            URL fxmlUrl = getClass().getResource("/com/financemanager/view/StartScreen.fxml");
            if (fxmlUrl == null) {
                System.err.println("Cannot find FXML file: /com/financemanager/view/StartScreen.fxml");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            // Get the controller and pass services
            StartScreenController controller = loader.getController();
            controller.setServices(transactionManager, budgetManager, transactionClassifier, expenseAnalyzer, aiService); // Pass AIService

            Scene scene = new Scene(root, 1200, 800); // Updated size
            primaryStage.setTitle("个人财务管理器");
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            // Consider showing an error dialog to the user
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
