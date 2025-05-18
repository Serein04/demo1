package com.financemanager.view;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import com.financemanager.ai.ExpenseAnalyzer; // Added import
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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class TransactionViewController {

    @FXML
    private BorderPane transactionRootPane;
    @FXML
    private Button backButton;
    @FXML
    private TableView<TransactionRow> transactionTable;
    @FXML
    private TableColumn<TransactionRow, String> idColumn;
    @FXML
    private TableColumn<TransactionRow, String> amountColumn;
    @FXML
    private TableColumn<TransactionRow, String> dateColumn;
    @FXML
    private TableColumn<TransactionRow, String> categoryColumn;
    @FXML
    private TableColumn<TransactionRow, String> descriptionColumn;
    @FXML
    private TableColumn<TransactionRow, String> typeColumn;
    @FXML
    private TableColumn<TransactionRow, String> paymentMethodColumn;
    @FXML
    private ToggleButton expenseToggleButton;
    @FXML
    private ToggleButton incomeToggleButton;
    @FXML
    private ToggleGroup typeToggleGroup;
    @FXML
    private TextField amountField;
    @FXML
    private DatePicker datePicker;
    @FXML
    private ComboBox<String> categoryComboBox;
    @FXML
    private Label paymentMethodLabel;
    @FXML
    private ComboBox<String> paymentMethodComboBox;
    @FXML
    private TextField descriptionField;
    @FXML
    private Button addButton;
    @FXML
    private Button editButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button importButton;

    private TransactionManager transactionManager;
    private TransactionClassifier transactionClassifier;
    private BudgetManager budgetManager;
    private ExpenseAnalyzer expenseAnalyzer; // Added ExpenseAnalyzer field
    // private MainFrame mainFrame; // In JavaFX, navigation is handled differently

    private static final String[] PAYMENT_METHODS = {"现金", "信用卡", "借记卡", "微信", "支付宝", "其他"};
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private ObservableList<TransactionRow> transactionData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // TODO: Properly initialize TransactionManager and TransactionClassifier
        // This might involve passing them from the main application or using a dependency injection framework.
        // For now, let's assume they are available or create new instances for basic functionality.
        // This is a placeholder and needs to be addressed for full functionality.
        // transactionManager = new TransactionManager();
        // transactionClassifier = new TransactionClassifier(new com.financemanager.ai.AIService());


        // Configure TableView columns
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        paymentMethodColumn.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));
        transactionTable.setItems(transactionData);

        // Initialize ComboBoxes
        paymentMethodComboBox.getItems().addAll(PAYMENT_METHODS);
        if (PAYMENT_METHODS.length > 0) {
            paymentMethodComboBox.setValue(PAYMENT_METHODS[0]);
        }
        
        // Set default type to expense
        expenseToggleButton.setSelected(true);
        updateCategoryComboBox(true);
        paymentMethodLabel.setVisible(true);
        paymentMethodComboBox.setVisible(true);
        
        datePicker.setValue(LocalDate.now());

        // loadTransactions(); // Call this once managers are properly initialized
        System.out.println("TransactionViewController initialized. TransactionManager and Classifier need proper injection.");
    }
    
    // This method should be called from where this view is created, passing the necessary services.
    public void setServices(TransactionManager tm, BudgetManager bm, TransactionClassifier tc, ExpenseAnalyzer ea) {
        this.transactionManager = tm;
        this.budgetManager = bm; 
        this.transactionClassifier = tc;
        this.expenseAnalyzer = ea; // Store ExpenseAnalyzer
        // Now that services are set, load initial data
        loadTransactions();
        updateCategoryComboBox(expenseToggleButton.isSelected()); // Initial category load based on default toggle
    }


    @FXML
    private void handleBackButtonAction(ActionEvent event) {
        try {
            Stage stage = (Stage) backButton.getScene().getWindow();
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

    @FXML
    private void handleExpenseToggleAction(ActionEvent event) {
        if (expenseToggleButton.isSelected()) {
            updateCategoryComboBox(true);
            paymentMethodLabel.setVisible(true);
            paymentMethodComboBox.setVisible(true);
        } else if (!incomeToggleButton.isSelected()){ // Ensure one is always selected
            expenseToggleButton.setSelected(true); 
        }
    }

    @FXML
    private void handleIncomeToggleAction(ActionEvent event) {
        if (incomeToggleButton.isSelected()) {
            updateCategoryComboBox(false);
            paymentMethodLabel.setVisible(false);
            paymentMethodComboBox.setVisible(false);
        } else if (!expenseToggleButton.isSelected()){ // Ensure one is always selected
             incomeToggleButton.setSelected(true);
        }
    }
    
    private void updateCategoryComboBox(boolean isExpense) {
        if (transactionClassifier == null) {
            System.err.println("TransactionClassifier not initialized in updateCategoryComboBox.");
            categoryComboBox.getItems().clear();
            categoryComboBox.setPromptText("分类加载失败");
            return;
        }
        categoryComboBox.getItems().clear();
        List<String> categories;
        if (isExpense) {
            categories = transactionClassifier.getExpenseCategories();
        } else {
            categories = transactionClassifier.getIncomeCategories();
        }
        categoryComboBox.getItems().addAll(categories);
        if (!categories.isEmpty()) {
            categoryComboBox.setValue(categories.get(0));
        }
    }

    private void loadTransactions() {
        if (transactionManager == null) {
            System.err.println("TransactionManager not initialized in loadTransactions.");
            transactionData.clear();
            // Optionally show an error to the user in the UI
            return;
        }
        transactionData.clear();
        List<Transaction> transactions = transactionManager.getAllTransactions();
        for (Transaction t : transactions) {
            transactionData.add(new TransactionRow(t));
        }
        // TODO: Call to refresh budget panel if it exists and is listening
    }

    @FXML
    private void handleAddTransaction(ActionEvent event) {
        if (transactionManager == null || transactionClassifier == null) {
            showAlert("错误", "服务未初始化，无法添加交易。");
            return;
        }
        try {
            double amountVal = Double.parseDouble(amountField.getText());
            LocalDate dateVal = datePicker.getValue();
            String categoryVal = categoryComboBox.getValue();
            String descriptionVal = descriptionField.getText();
            boolean isExpense = expenseToggleButton.isSelected();
            String paymentMethodVal = isExpense ? paymentMethodComboBox.getValue() : "";

            if (dateVal == null || categoryVal == null) {
                showAlert("输入错误", "日期和类别不能为空。");
                return;
            }

            Transaction transaction = new Transaction(amountVal, dateVal, categoryVal, descriptionVal, isExpense, paymentMethodVal);
            transactionManager.addTransaction(transaction);
            
            // AI Classification (optional, similar to Swing)
            try {
                String aiCategory = transactionClassifier.classifyTransaction(transaction);
                if (!aiCategory.equals(categoryVal)) {
                    transactionClassifier.learnFromUserCorrection(transaction, aiCategory, categoryVal);
                }
                 showAlert("成功", "交易记录添加成功。");
            } catch (Exception aiEx) {
                 showAlert("AI分类警告", String.format("交易记录已添加,但AI分类出现错误:%s\n请检查交易描述是否完整。", aiEx.getMessage()));
            }

            loadTransactions();
            clearInputFields();
        } catch (NumberFormatException e) {
            showAlert("输入错误", "请输入有效的金额。");
        } catch (Exception e) {
            showAlert("错误", "添加交易记录时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleEditTransaction(ActionEvent event) {
        TransactionRow selectedRow = transactionTable.getSelectionModel().getSelectedItem();
        if (selectedRow == null) {
            showAlert("提示", "请先选择要编辑的交易记录。");
            return;
        }
        if (transactionManager == null) {
             showAlert("错误", "服务未初始化，无法编辑交易。");
            return;
        }

        Transaction selectedTransaction = transactionManager.getAllTransactions().stream()
                .filter(t -> t.getId().equals(selectedRow.getId()))
                .findFirst()
                .orElse(null);

        if (selectedTransaction != null) {
            // For JavaFX, we'd typically open a new dialog window (another FXML + Controller)
            // This is a simplified placeholder. A proper edit dialog is needed.
            // For now, let's just re-populate fields and change add button to save.
            // This is NOT a good UX for editing, just a temporary measure.
            // A true edit dialog (like TransactionEditDialog in Swing) should be created.
            
            amountField.setText(String.format("%.2f", selectedTransaction.getAmount()));
            datePicker.setValue(selectedTransaction.getDate());
            descriptionField.setText(selectedTransaction.getDescription());
            if (selectedTransaction.isExpense()) {
                expenseToggleButton.setSelected(true);
                handleExpenseToggleAction(null); // Update UI state
                paymentMethodComboBox.setValue(selectedTransaction.getPaymentMethod());
            } else {
                incomeToggleButton.setSelected(true);
                handleIncomeToggleAction(null); // Update UI state
            }
            categoryComboBox.setValue(selectedTransaction.getCategory()); // Ensure categories are loaded for the type

            // Temporarily change Add button to Save, and implement save logic
            // This is a hack. A dedicated dialog is better.
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "编辑功能需要一个专用的对话框。当前实现为占位符。", ButtonType.OK);
            alert.setHeaderText("编辑功能提示");
            alert.showAndWait();

            // TODO: Implement a proper TransactionEditDialog for JavaFX
            // Example:
            // try {
            // FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/financemanager/view/TransactionEditDialog.fxml"));
            // Parent dialogRoot = loader.load();
            // TransactionEditDialogController controller = loader.getController();
            // controller.setTransaction(selectedTransaction, transactionManager, transactionClassifier);
            // Stage dialogStage = new Stage();
            // dialogStage.setTitle("编辑交易");
            // dialogStage.initModality(Modality.WINDOW_MODAL);
            // dialogStage.initOwner(transactionRootPane.getScene().getWindow());
            // Scene dialogScene = new Scene(dialogRoot);
            // dialogStage.setScene(dialogScene);
            // dialogStage.showAndWait();
            // if (controller.isConfirmed()) {
            // loadTransactions();
            // }
            // } catch (IOException e) {
            // e.printStackTrace();
            // showAlert("Error", "Could not open edit dialog.");
            // }

        } else {
            showAlert("错误", "无法加载选中的交易记录。");
        }
    }

    @FXML
    private void handleDeleteTransaction(ActionEvent event) {
        TransactionRow selectedRow = transactionTable.getSelectionModel().getSelectedItem();
        if (selectedRow == null) {
            showAlert("提示", "请先选择要删除的交易记录。");
            return;
        }
         if (transactionManager == null) {
             showAlert("错误", "服务未初始化，无法删除交易。");
            return;
        }

        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION, "确定要删除选中的交易记录吗？", ButtonType.YES, ButtonType.NO);
        confirmDialog.setHeaderText("确认删除");
        Optional<ButtonType> result = confirmDialog.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.YES) {
            if (transactionManager.removeTransaction(selectedRow.getId())) {
                loadTransactions();
            } else {
                showAlert("错误", "删除失败，未找到该记录。");
            }
        }
    }

    @FXML
    private void handleImportCSV(ActionEvent event) {
        if (transactionManager == null) {
             showAlert("错误", "服务未初始化，无法导入CSV。");
            return;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择CSV文件导入");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV 文件", "*.csv"));
        File selectedFile = fileChooser.showOpenDialog(transactionRootPane.getScene().getWindow());

        if (selectedFile != null) {
            try {
                int importedCount = transactionManager.importFromCSV(selectedFile.getAbsolutePath());
                showAlert("导入成功", "成功导入 " + importedCount + " 条交易记录。");
                loadTransactions();
            } catch (Exception e) {
                showAlert("导入错误", "导入CSV文件时出错: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void clearInputFields() {
        amountField.clear();
        descriptionField.clear();
        datePicker.setValue(LocalDate.now());
        // Reset category and payment method to defaults if needed
        if (!categoryComboBox.getItems().isEmpty()) {
            categoryComboBox.setValue(categoryComboBox.getItems().get(0));
        }
        if (!paymentMethodComboBox.getItems().isEmpty()) {
            paymentMethodComboBox.setValue(paymentMethodComboBox.getItems().get(0));
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // Helper class for TableView data
    // The Transaction class itself could be used if its properties are JavaFX properties,
    // or if we add getters that match PropertyValueFactory conventions.
    // Using a dedicated Row class is often cleaner for UI-specific formatting.
    public static class TransactionRow {
        private final Transaction transaction;

        public TransactionRow(Transaction transaction) {
            this.transaction = transaction;
        }

        public String getId() { return transaction.getId(); }
        public String getAmount() { return String.format("%.2f", transaction.getAmount()); }
        public String getDate() { return transaction.getDate().format(DATE_FORMATTER); }
        public String getCategory() { return transaction.getCategory(); }
        public String getDescription() { return transaction.getDescription(); }
        public String getType() { return transaction.isExpense() ? "支出" : "收入"; }
        public String getPaymentMethod() { return transaction.getPaymentMethod(); }
        // Original Transaction object if needed for operations like edit/delete
        public Transaction getOriginalTransaction() { return transaction; }
    }
}
