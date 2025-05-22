package com.financemanager.view;

import java.io.IOException;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.financemanager.ai.AIService; // Import AIService
import com.financemanager.ai.ExpenseAnalyzer;
import com.financemanager.ai.TransactionClassifier;
import com.financemanager.model.BudgetManager;
import com.financemanager.model.Transaction;
import com.financemanager.model.TransactionManager;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font; // Added import for HashMap
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class AnalysisViewController {

    @FXML
    private Button backButton;
    @FXML
    private Button generateReportButton;
    @FXML
    private VBox reportContentVBox;

    private TransactionManager transactionManager;
    private BudgetManager budgetManager; // For consistent navigation back
    private TransactionClassifier transactionClassifier; // For consistent navigation back
    private ExpenseAnalyzer expenseAnalyzer;
    private AIService aiService; // Add AIService field

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Font TITLE_FONT = Font.font("System", FontWeight.BOLD, 18);
    private static final Font TEXT_FONT = Font.font("System", FontWeight.NORMAL, 14);


    @FXML
    public void initialize() {
        System.out.println("AnalysisViewController initialized. Services need proper injection.");
        // Report generation will be triggered by setServices after services are injected.
    }

    public void setServices(TransactionManager tm, BudgetManager bm, TransactionClassifier tc, ExpenseAnalyzer ea, AIService as) {
        this.transactionManager = tm;
        this.budgetManager = bm;
        this.transactionClassifier = tc;
        this.expenseAnalyzer = ea;
        this.aiService = as; // Store AIService

        // Automatically generate report now that services are injected
        // We pass null as ActionEvent because it's not used in the method.
        handleGenerateReportAction(null);
    }

    @FXML
    private void handleGenerateReportAction(ActionEvent event) {
        if (transactionManager == null || expenseAnalyzer == null) {
            showAlert("错误", "服务未初始化，无法生成报告。");
            return;
        }

        List<Transaction> transactions = transactionManager.getAllTransactions();
        if (transactions.isEmpty()) {
            showAlert("提示", "没有交易记录可供分析。");
            reportContentVBox.getChildren().clear();
            Label noDataLabel = new Label("没有交易记录可供分析。");
            noDataLabel.setFont(TEXT_FONT);
            reportContentVBox.getChildren().add(noDataLabel);
            return;
        }

        reportContentVBox.getChildren().clear();
        reportContentVBox.setSpacing(20); // Add more spacing between sections

        // Monthly Spending Trend
        addSectionTitle("月度支出趋势");
        Map<YearMonth, Double> monthlyTrend = expenseAnalyzer.analyzeMonthlyTrend(transactions);
        if (!monthlyTrend.isEmpty()) {
            PieChart monthlyChart = createPieChartFromYearMonthData(monthlyTrend, "月度支出分布");
            reportContentVBox.getChildren().add(monthlyChart);
        } else {
            addNoDataLabel("无月度支出数据。");
        }

        // Category Spending Distribution
        addSectionTitle("支出类别分布");
        Map<String, Double> categorySpending = calculateLocalCategorySpending(transactions); // Use local helper method
        if (!categorySpending.isEmpty()) {
            PieChart categoryChart = createPieChartFromStringData(categorySpending, "支出类别分布");
            reportContentVBox.getChildren().add(categoryChart);
        } else {
            addNoDataLabel("无类别支出数据。");
        }
        
        // Abnormal Expenses
        addSectionTitle("异常支出检测");
        List<Transaction> abnormalExpenses = expenseAnalyzer.detectAbnormalExpenses(transactions);
        if (abnormalExpenses.isEmpty()) {
            addNoDataLabel("未检测到异常支出。");
        } else {
            TableView<Transaction> abnormalExpensesTable = new TableView<>();
            abnormalExpensesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

            TableColumn<Transaction, String> dateCol = new TableColumn<>("日期");
            dateCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDate().format(DATE_FORMATTER)));

            TableColumn<Transaction, String> categoryCol = new TableColumn<>("类别");
            categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));

            TableColumn<Transaction, Double> amountCol = new TableColumn<>("金额");
            amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
            amountCol.setCellFactory(column -> new TableCell<Transaction, Double>() {
                @Override
                protected void updateItem(Double item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(String.format("%.2f", item));
                    }
                }
            });
            
            TableColumn<Transaction, String> descriptionCol = new TableColumn<>("描述");
            descriptionCol.setCellValueFactory(new PropertyValueFactory<>("description"));


            abnormalExpensesTable.getColumns().addAll(dateCol, categoryCol, amountCol, descriptionCol);
            abnormalExpensesTable.setItems(FXCollections.observableArrayList(abnormalExpenses));
            // Set preferred height for the table to avoid it taking too much space or too little
            abnormalExpensesTable.setPrefHeight(250); // Adjust as needed, e.g., 30 per row + header
            reportContentVBox.getChildren().add(abnormalExpensesTable);
        }

        // Seasonal Patterns
        addSectionTitle("季节性支出模式");
        Map<Month, List<String>> seasonalPatterns = expenseAnalyzer.detectSeasonalPatterns(transactions);
        if (seasonalPatterns.isEmpty()) {
            addNoDataLabel("未检测到明显的季节性支出模式。");
        } else {
            TableView<SeasonalPatternRow> seasonalPatternsTable = new TableView<>();
            seasonalPatternsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            
            TableColumn<SeasonalPatternRow, String> monthCol = new TableColumn<>("月份");
            monthCol.setCellValueFactory(new PropertyValueFactory<>("month"));
            
            TableColumn<SeasonalPatternRow, String> categoriesCol = new TableColumn<>("相关支出类别");
            categoriesCol.setCellValueFactory(new PropertyValueFactory<>("categories"));
            
            seasonalPatternsTable.getColumns().addAll(monthCol, categoriesCol);
            
            ObservableList<SeasonalPatternRow> seasonalData = FXCollections.observableArrayList();
            for (Map.Entry<Month, List<String>> entry : seasonalPatterns.entrySet()) {
                seasonalData.add(new SeasonalPatternRow(entry.getKey().toString(), String.join(", ", entry.getValue())));
            }
            seasonalPatternsTable.setItems(seasonalData);
            seasonalPatternsTable.setPrefHeight(250); // Adjust as needed
            reportContentVBox.getChildren().add(seasonalPatternsTable);
        }
        
        // Saving Opportunities
        addSectionTitle("节省机会");
        List<Map<String, Object>> savingOpportunities = expenseAnalyzer.analyzeSavingOpportunities(transactions);
        if (savingOpportunities.isEmpty()) {
            addNoDataLabel("未发现明显的节省机会。");
        } else {
            VBox opportunitiesList = new VBox(5);
            for (Map<String, Object> opportunity : savingOpportunities) {
                Label l = new Label(String.format("  • %s", opportunity.get("description")));
                l.setFont(TEXT_FONT);
                opportunitiesList.getChildren().add(l);
            }
            reportContentVBox.getChildren().add(opportunitiesList);
        }
    }
    
    private void addSectionTitle(String title) {
        Label titleLabel = new Label(title);
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setPadding(new Insets(10, 0, 5, 0));
        reportContentVBox.getChildren().add(titleLabel);
    }

    private void addNoDataLabel(String message) {
        Label noDataLabel = new Label(message);
        noDataLabel.setFont(TEXT_FONT);
        noDataLabel.setPadding(new Insets(0,0,10,5));
        reportContentVBox.getChildren().add(noDataLabel);
    }

    private PieChart createPieChartFromYearMonthData(Map<YearMonth, Double> dataMap, String title) {
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        for (Map.Entry<YearMonth, Double> entry : dataMap.entrySet()) {
            if (entry.getValue() > 0) {
                 pieChartData.add(new PieChart.Data(entry.getKey().toString() + String.format(" (%.2f)", entry.getValue()), entry.getValue()));
            }
        }
        PieChart chart = new PieChart(pieChartData);
        chart.setTitle(title);
        chart.setPrefHeight(300);
        return chart;
    }
    
    private PieChart createPieChartFromStringData(Map<String, Double> dataMap, String title) {
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        for (Map.Entry<String, Double> entry : dataMap.entrySet()) {
             if (entry.getValue() > 0) {
                pieChartData.add(new PieChart.Data(entry.getKey() + String.format(" (%.2f)", entry.getValue()), entry.getValue()));
             }
        }
        PieChart chart = new PieChart(pieChartData);
        chart.setTitle(title);
        chart.setPrefHeight(300);
        return chart;
    }


    @FXML
    private void handleBackButtonAction(ActionEvent event) {
        try {
            Stage stage = (Stage) backButton.getScene().getWindow();
            // Attempt to detach old root explicitly
            if (stage.getScene() != null && stage.getScene().getRoot() != null) {
                stage.getScene().setRoot(new javafx.scene.layout.Pane());
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/financemanager/view/StartScreen.fxml"));
            Parent startScreenRoot = loader.load();
            StartScreenController ssc = loader.getController();
            // Pass all services needed by StartScreenController and its subsequent views
            ssc.setServices(transactionManager, budgetManager, transactionClassifier, expenseAnalyzer, aiService); 
            stage.setScene(new Scene(startScreenRoot, 1200, 800)); // Updated size
            stage.setTitle("个人财务管理器");
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

    // Helper method to calculate category spending, similar to the original Swing panel
    private Map<String, Double> calculateLocalCategorySpending(List<Transaction> transactions) {
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

    // Inner class for Seasonal Patterns TableView
    public static class SeasonalPatternRow {
        private final SimpleStringProperty month;
        private final SimpleStringProperty categories;

        public SeasonalPatternRow(String month, String categories) {
            this.month = new SimpleStringProperty(month);
            this.categories = new SimpleStringProperty(categories);
        }

        public String getMonth() {
            return month.get();
        }

        public SimpleStringProperty monthProperty() {
            return month;
        }

        public String getCategories() {
            return categories.get();
        }

        public SimpleStringProperty categoriesProperty() {
            return categories;
        }
    }
}
