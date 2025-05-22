package com.financemanager.view;

import java.io.IOException;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.financemanager.ai.AIService; // Import AIService
import com.financemanager.ai.ExpenseAnalyzer;
import com.financemanager.ai.TransactionClassifier;
import com.financemanager.model.BudgetManager;
import com.financemanager.model.Transaction;
import com.financemanager.model.TransactionManager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory; // Added for custom cell formatting
import javafx.stage.Stage; // Added for custom cell formatting

public class BudgetViewController {

    @FXML
    private Button backButton;
    @FXML
    private TextField monthlyBudgetField;
    @FXML
    private Button setMonthlyBudgetButton;
    @FXML
    private TextField savingsGoalField;
    @FXML
    private Button setSavingsGoalButton;
    @FXML
    private ComboBox<String> budgetCategoryComboBox;
    @FXML
    private TextField categoryBudgetField;
    @FXML
    private Button setCategoryBudgetButton;
    @FXML
    private TableView<BudgetRow> budgetTable;
    @FXML
    private TableColumn<BudgetRow, String> categoryColumn;
    @FXML
    private TableColumn<BudgetRow, Double> budgetAmountColumn; // Changed to Double
    @FXML
    private TableColumn<BudgetRow, Double> currentSpendingColumn; // Changed to Double
    @FXML
    private TableColumn<BudgetRow, Double> remainingAmountColumn; // Changed to Double
    @FXML
    private TableColumn<BudgetRow, String> statusColumn;
    @FXML
    private PieChart budgetPieChart;

    private TransactionManager transactionManager;
    private BudgetManager budgetManager;
    private TransactionClassifier transactionClassifier;
    private ExpenseAnalyzer expenseAnalyzer;
    private AIService aiService; // Add AIService field

    private ObservableList<BudgetRow> budgetTableData = FXCollections.observableArrayList();
    private ObservableList<PieChart.Data> budgetPieChartData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Configure TableView columns
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));

        // Budget Amount Column
        budgetAmountColumn.setCellValueFactory(new PropertyValueFactory<>("budgetAmountNumeric"));
        budgetAmountColumn.setCellFactory(column -> new TableCell<BudgetRow, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    // Check if the row is for an unbudgeted item or "总计" to decide format
                    BudgetRow row = getTableView().getItems().get(getIndex());
                    if (row.isUnbudgeted() && item == 0.0) {
                        setText("未设置");
                    } else {
                        setText(String.format("%.2f", item));
                    }
                }
            }
        });

        // Current Spending Column
        currentSpendingColumn.setCellValueFactory(new PropertyValueFactory<>("currentSpendingNumeric"));
        currentSpendingColumn.setCellFactory(column -> new TableCell<BudgetRow, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("%.2f", item));
            }
        });

        // Remaining Amount Column
        remainingAmountColumn.setCellValueFactory(new PropertyValueFactory<>("remainingAmountNumeric"));
        remainingAmountColumn.setCellFactory(column -> new TableCell<BudgetRow, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                     BudgetRow row = getTableView().getItems().get(getIndex());
                    if (row.isUnbudgeted()) {
                         setText("N/A");
                    } else {
                        setText(String.format("%.2f", item));
                    }
                }
            }
        });
        
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        budgetTable.setItems(budgetTableData);

        budgetTable.setRowFactory(tv -> new TableRow<BudgetRow>() {
            @Override
            protected void updateItem(BudgetRow item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else {
                    // Check if the status is "超支"
                    if ("超支".equals(item.getStatus())) {
                        setStyle("-fx-background-color: #FFCDD2;"); // Light red color for over-budget rows
                    } else {
                        setStyle(""); // Default style
                    }
                }
            }
        });

        // Configure PieChart
        budgetPieChart.setData(budgetPieChartData);
        budgetPieChart.setLegendVisible(true);
        budgetPieChart.setLabelsVisible(true); // Show labels on slices

        System.out.println("BudgetViewController initialized. Services need proper injection.");
    }

    public void setServices(TransactionManager tm, BudgetManager bm, TransactionClassifier tc, ExpenseAnalyzer ea, AIService as) {
        this.transactionManager = tm;
        this.budgetManager = bm;
        this.transactionClassifier = tc;
        this.expenseAnalyzer = ea;
        this.aiService = as; // Store AIService

        populateCategoryComboBox();
        loadBudgetData();
    }

    private void populateCategoryComboBox() {
        if (transactionClassifier != null) {
            budgetCategoryComboBox.getItems().clear();
            budgetCategoryComboBox.getItems().addAll(transactionClassifier.getExpenseCategories());
            if (!budgetCategoryComboBox.getItems().isEmpty()) {
                budgetCategoryComboBox.getSelectionModel().selectFirst();
                // Display budget for the initially selected category
                displayCategoryBudget(); 
            }
        } else {
            System.err.println("TransactionClassifier is null in populateCategoryComboBox.");
        }
         // Add listener to update categoryBudgetField when selection changes
        budgetCategoryComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                displayCategoryBudget();
            }
        });
    }
    
    private void displayCategoryBudget() {
        String selectedCategory = budgetCategoryComboBox.getValue();
        if (selectedCategory != null && budgetManager != null) {
            double budget = budgetManager.getCategoryBudget(selectedCategory);
            categoryBudgetField.setText(String.format("%.2f", budget));
        } else {
            categoryBudgetField.setText("");
        }
    }


    private void loadBudgetData() {
        if (budgetManager == null || transactionManager == null) {
            System.err.println("Managers not initialized in loadBudgetData.");
            return;
        }
        monthlyBudgetField.setText(String.format("%.2f", budgetManager.getMonthlyBudget()));
        savingsGoalField.setText(String.format("%.2f", budgetManager.getSavingsGoal()));
        displayCategoryBudget(); // Ensure category budget field is updated

        updateBudgetTable();
        updateBudgetPieChart();
    }

    private void updateBudgetTable() {
        budgetTableData.clear();
        YearMonth currentMonth = YearMonth.now();
        Map<String, Double> categorySpending = new HashMap<>();
        List<Transaction> transactions = transactionManager.getAllTransactions();

        for (Transaction t : transactions) {
            if (t.isExpense() && YearMonth.from(t.getDate()).equals(currentMonth)) {
                categorySpending.put(t.getCategory(),
                        categorySpending.getOrDefault(t.getCategory(), 0.0) + t.getAmount());
            }
        }

        Map<String, Double> categoryBudgets = budgetManager.getAllCategoryBudgets();
        double totalSpending = categorySpending.values().stream().mapToDouble(Double::doubleValue).sum();
        double monthlyBudget = budgetManager.getMonthlyBudget();
        double remainingOverall = monthlyBudget - totalSpending;
        budgetTableData.add(new BudgetRow("总计", monthlyBudget, totalSpending, remainingOverall));

        for (Map.Entry<String, Double> entry : categoryBudgets.entrySet()) {
            String category = entry.getKey();
            double budget = entry.getValue();
            double spending = categorySpending.getOrDefault(category, 0.0);
            budgetTableData.add(new BudgetRow(category, budget, spending, budget - spending));
        }

        for (String category : categorySpending.keySet()) {
            if (!categoryBudgets.containsKey(category)) {
                double spending = categorySpending.get(category);
                budgetTableData.add(new BudgetRow(category, 0.0, spending, -spending, true)); // Mark as unbudgeted
            }
        }
    }

    private void updateBudgetPieChart() {
        budgetPieChartData.clear();
        YearMonth currentMonth = YearMonth.now();
        Map<String, Double> categorySpending = new HashMap<>();
        List<Transaction> transactions = transactionManager.getAllTransactions();

        for (Transaction t : transactions) {
            if (t.isExpense() && YearMonth.from(t.getDate()).equals(currentMonth)) {
                categorySpending.put(t.getCategory(),
                        categorySpending.getOrDefault(t.getCategory(), 0.0) + t.getAmount());
            }
        }

        if (categorySpending.isEmpty()) {
            budgetPieChart.setTitle("当月无支出数据");
        } else {
            budgetPieChart.setTitle("当月支出分布"); // Reset title if data exists
            for (Map.Entry<String, Double> entry : categorySpending.entrySet()) {
                if (entry.getValue() > 0) { // Only add categories with spending
                    budgetPieChartData.add(new PieChart.Data(entry.getKey() + String.format(" (%.2f)", entry.getValue()), entry.getValue()));
                }
            }
        }
    }

    @FXML
    private void handleSetMonthlyBudget(ActionEvent event) {
        try {
            double amount = Double.parseDouble(monthlyBudgetField.getText());
            budgetManager.setMonthlyBudget(amount);
            showAlert("成功", "月度预算设置成功。");
            loadBudgetData();
        } catch (NumberFormatException e) {
            showAlert("输入错误", "请输入有效的金额。");
        }
    }

    @FXML
    private void handleSetSavingsGoal(ActionEvent event) {
        try {
            double amount = Double.parseDouble(savingsGoalField.getText());
            budgetManager.setSavingsGoal(amount);
            showAlert("成功", "储蓄目标设置成功。");
            loadBudgetData(); // Savings goal doesn't directly affect table/chart but good to refresh all
        } catch (NumberFormatException e) {
            showAlert("输入错误", "请输入有效的金额。");
        }
    }

    @FXML
    private void handleSetCategoryBudget(ActionEvent event) {
        try {
            String category = budgetCategoryComboBox.getValue();
            if (category == null || category.isEmpty()) {
                showAlert("提示", "请先选择一个类别。");
                return;
            }
            double amount = Double.parseDouble(categoryBudgetField.getText());
            budgetManager.setCategoryBudget(category, amount);
            showAlert("成功", "类别预算 '" + category + "' 设置成功。");
            loadBudgetData();
        } catch (NumberFormatException e) {
            showAlert("输入错误", "请输入有效的金额。");
        }
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
            // Pass all five services
            ssc.setServices(transactionManager, budgetManager, transactionClassifier, expenseAnalyzer, aiService); 

            Scene newScene = new Scene(startScreenRoot, 1200, 800); // Updated size
            stage.setScene(newScene);
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

    public static class BudgetRow {
        private final String category;
        private final double budgetAmount;
        private final double currentSpending;
        private final double remainingAmount;
        private final String status;
        private final boolean isUnbudgeted;

        public BudgetRow(String category, double budgetAmount, double currentSpending, double remainingAmount, boolean isUnbudgeted) {
            this.category = category;
            this.budgetAmount = budgetAmount;
            this.currentSpending = currentSpending;
            this.remainingAmount = remainingAmount;
            this.isUnbudgeted = isUnbudgeted;
            if (isUnbudgeted) {
                this.status = "未设置预算";
            } else {
                this.status = remainingAmount >= 0 ? "正常" : "超支";
            }
        }
        public BudgetRow(String category, double budgetAmount, double currentSpending, double remainingAmount) {
            this(category, budgetAmount, currentSpending, remainingAmount, false);
        }


        public String getCategory() { return category; }
        // Original String returning getters for PropertyValueFactory if still used elsewhere or for non-numeric display needs
        public String getBudgetAmountString() { return isUnbudgeted && budgetAmount == 0.0 ? "未设置" : String.format("%.2f", budgetAmount); }
        public String getCurrentSpendingString() { return String.format("%.2f", currentSpending); }
        public String getRemainingAmountString() { return isUnbudgeted ? "N/A" : String.format("%.2f", remainingAmount); }
        public String getStatus() { return status; }

        // Numeric getters for sorting and typed columns
        public double getBudgetAmountNumeric() { return budgetAmount; }
        public double getCurrentSpendingNumeric() { return currentSpending; }
        public double getRemainingAmountNumeric() { return remainingAmount; }
        public boolean isUnbudgeted() { return isUnbudgeted; } // Getter for isUnbudgeted
    }
}
