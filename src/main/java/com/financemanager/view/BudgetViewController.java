package com.financemanager.view;

import java.io.IOException;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.financemanager.ai.ExpenseAnalyzer; // Import ExpenseAnalyzer
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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

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
    private TableColumn<BudgetRow, String> budgetAmountColumn;
    @FXML
    private TableColumn<BudgetRow, String> currentSpendingColumn;
    @FXML
    private TableColumn<BudgetRow, String> remainingAmountColumn;
    @FXML
    private TableColumn<BudgetRow, String> statusColumn;
    @FXML
    private PieChart budgetPieChart;

    private TransactionManager transactionManager;
    private BudgetManager budgetManager;
    private TransactionClassifier transactionClassifier;
    private ExpenseAnalyzer expenseAnalyzer; // Add ExpenseAnalyzer field

    private ObservableList<BudgetRow> budgetTableData = FXCollections.observableArrayList();
    private ObservableList<PieChart.Data> budgetPieChartData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Configure TableView columns
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        budgetAmountColumn.setCellValueFactory(new PropertyValueFactory<>("budgetAmount"));
        currentSpendingColumn.setCellValueFactory(new PropertyValueFactory<>("currentSpending"));
        remainingAmountColumn.setCellValueFactory(new PropertyValueFactory<>("remainingAmount"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        budgetTable.setItems(budgetTableData);

        // Configure PieChart
        budgetPieChart.setData(budgetPieChartData);
        budgetPieChart.setLegendVisible(true);
        budgetPieChart.setLabelsVisible(true); // Show labels on slices

        System.out.println("BudgetViewController initialized. Services need proper injection.");
    }

    public void setServices(TransactionManager tm, BudgetManager bm, TransactionClassifier tc, ExpenseAnalyzer ea) {
        this.transactionManager = tm;
        this.budgetManager = bm;
        this.transactionClassifier = tc;
        this.expenseAnalyzer = ea; // Store ExpenseAnalyzer

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
            Parent root = FXMLLoader.load(getClass().getResource("/com/financemanager/view/StartScreen.fxml"));
            // We need to pass services back to StartScreenController if it's re-created this way,
            // or better, have a main navigation controller.
            // For simplicity now, this just loads StartScreen.
            // A more robust navigation would re-use the StartScreenController instance or pass services.
            
            // To pass services correctly if StartScreenController is re-instantiated:
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/financemanager/view/StartScreen.fxml"));
            Parent startScreenRoot = loader.load();
            StartScreenController ssc = loader.getController();
            // Pass all four services
            ssc.setServices(transactionManager, budgetManager, transactionClassifier, expenseAnalyzer); 

            Scene scene = new Scene(startScreenRoot);
            stage.setScene(scene);
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
        public String getBudgetAmount() { return isUnbudgeted && budgetAmount == 0.0 ? "未设置" : String.format("%.2f", budgetAmount); }
        public String getCurrentSpending() { return String.format("%.2f", currentSpending); }
        public String getRemainingAmount() { return isUnbudgeted ? "N/A" : String.format("%.2f", remainingAmount); }
        public String getStatus() { return status; }
    }
}
