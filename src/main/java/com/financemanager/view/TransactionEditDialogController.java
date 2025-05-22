package com.financemanager.view;

import java.time.LocalDate;
import java.util.List;

import com.financemanager.ai.TransactionClassifier; // Assuming this is needed for category learning
import com.financemanager.model.Transaction;
import com.financemanager.model.TransactionManager;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;

public class TransactionEditDialogController {

    @FXML private ToggleButton expenseToggleButton;
    @FXML private ToggleButton incomeToggleButton;
    @FXML private ToggleGroup typeToggleGroup;
    @FXML private TextField amountField;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private Label paymentMethodLabel;
    @FXML private ComboBox<String> paymentMethodComboBox;
    @FXML private TextField descriptionField;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private Transaction transactionToEdit;
    private TransactionManager transactionManager;
    private TransactionClassifier transactionClassifier;
    private boolean confirmed = false;
    private String aiSuggestedCategoryFromCaller; // Store the AI suggestion passed from caller

    private static final String[] PAYMENT_METHODS = {"现金", "信用卡", "借记卡", "微信", "支付宝", "其他"};

    // UI element to display AI suggestion - to be added to FXML if not already there
    @FXML private Label aiSuggestionLabel; // Example: needs fx:id="aiSuggestionLabel" in FXML

    @FXML
    public void initialize() {
        paymentMethodComboBox.getItems().addAll(PAYMENT_METHODS);
        expenseToggleButton.setOnAction(event -> updatePaymentMethodVisibility(true));
        incomeToggleButton.setOnAction(event -> updatePaymentMethodVisibility(false));
        if (aiSuggestionLabel != null) { // Hide if no suggestion initially
            aiSuggestionLabel.setVisible(false);
            aiSuggestionLabel.setManaged(false);
        }
    }

    public void setTransaction(Transaction transaction, TransactionManager transactionManager, TransactionClassifier transactionClassifier, String aiSuggestion) {
        this.transactionToEdit = transaction;
        this.transactionManager = transactionManager;
        this.transactionClassifier = transactionClassifier;
        this.aiSuggestedCategoryFromCaller = aiSuggestion;

        if (aiSuggestionLabel != null && aiSuggestion != null && !aiSuggestion.isEmpty()) {
            aiSuggestionLabel.setText("AI 建议分类: " + aiSuggestion);
            aiSuggestionLabel.setVisible(true);
            aiSuggestionLabel.setManaged(true);
        } else if (aiSuggestionLabel != null) {
            aiSuggestionLabel.setVisible(false);
            aiSuggestionLabel.setManaged(false);
        }

        if (transactionToEdit != null) {
            amountField.setText(String.format("%.2f", transactionToEdit.getAmount()));
            datePicker.setValue(transactionToEdit.getDate());
            descriptionField.setText(transactionToEdit.getDescription());

            if (transactionToEdit.isExpense()) {
                expenseToggleButton.setSelected(true);
                updatePaymentMethodVisibility(true);
                paymentMethodComboBox.setValue(transactionToEdit.getPaymentMethod());
            } else {
                incomeToggleButton.setSelected(true);
                updatePaymentMethodVisibility(false);
            }
            updateCategoryComboBox(transactionToEdit.isExpense());
            categoryComboBox.setValue(transactionToEdit.getCategory());
        } else {
            // Default to new expense
            expenseToggleButton.setSelected(true);
            updatePaymentMethodVisibility(true);
            updateCategoryComboBox(true);
            datePicker.setValue(LocalDate.now());
            if (PAYMENT_METHODS.length > 0) {
                paymentMethodComboBox.setValue(PAYMENT_METHODS[0]);
            }
        }
    }
    
    private void updatePaymentMethodVisibility(boolean isExpense) {
        paymentMethodLabel.setVisible(isExpense);
        paymentMethodComboBox.setVisible(isExpense);
    }

    private void updateCategoryComboBox(boolean isExpense) {
        if (transactionClassifier == null) {
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
        if (!categories.isEmpty() && categoryComboBox.getValue() == null) {
             // Select first if no value is set or if current value not in new list
            if (transactionToEdit == null || !categories.contains(transactionToEdit.getCategory())) {
                 categoryComboBox.setValue(categories.get(0));
            }
        }
    }


    @FXML
    private void handleSaveAction(ActionEvent event) {
        if (!validateInput()) {
            return;
        }

        try {
            double amount = Double.parseDouble(amountField.getText());
            LocalDate date = datePicker.getValue();
            String category = categoryComboBox.getValue();
            String description = descriptionField.getText();
            boolean isExpense = expenseToggleButton.isSelected();
            String paymentMethod = isExpense ? paymentMethodComboBox.getValue() : "";

            String originalCategoryBeforeEdit = transactionToEdit.getCategory(); // Category before user potentially changes it in this dialog

            // Update existing transaction object
            transactionToEdit.setAmount(amount);
            transactionToEdit.setDate(date);
            transactionToEdit.setCategory(category);
            transactionToEdit.setDescription(description);
            transactionToEdit.setExpense(isExpense);
            transactionToEdit.setPaymentMethod(paymentMethod);
            
            // In a real scenario, TransactionManager might have an updateTransaction method.
            // For simplicity, if it only has add/remove, we might need to remove then add.
            // Let's assume TransactionManager can handle updates by ID or object reference.
            // If not, this part needs adjustment based on TransactionManager's capabilities.
            // For now, we assume the object passed to setTransaction is the one managed by TransactionManager
            // and modifying it directly is enough before a save/refresh in the manager.
            // A more robust way: transactionManager.updateTransaction(transactionToEdit);
            
            // Simulate update by removing old and adding new if no direct update method
            // This is a common pattern if objects are treated as immutable or manager uses internal copies.
            // However, if Transaction objects are managed by reference, direct modification is fine
            // and TransactionManager.saveTransactions() would persist it.
            // For this example, let's assume direct modification is fine and manager will save.
            // If TransactionManager.updateTransaction(Transaction) exists, use it.
            // If not, and if IDs are crucial and immutable, then remove by ID and add new.
            // Given the existing TransactionManager likely uses a list of Transaction objects,
            // modifying the transactionToEdit object directly should be reflected if the list holds references.
            // The crucial part is that TransactionManager.saveTransactions() is called after this dialog closes.

            // AI learning logic based on user's design
            if (aiSuggestedCategoryFromCaller != null && !aiSuggestedCategoryFromCaller.isEmpty()) {
                // This was a transaction flagged by AI
                if (!category.equals(aiSuggestedCategoryFromCaller)) { 
                    // User rejected AI's suggestion (or chose something else entirely)
                    // AI learns: user says 'category' is correct, not 'aiSuggestedCategoryFromCaller'
                    transactionClassifier.learnFromUserCorrection(transactionToEdit, aiSuggestedCategoryFromCaller, category);
                }
                // If category.equals(aiSuggestedCategoryFromCaller), user accepted AI suggestion, no learning needed.
            } else if (transactionClassifier != null && !originalCategoryBeforeEdit.equals(category)) {
                // This was a normal edit (not flagged by AI initially), but user changed category.
                // AI learns this change as a user preference.
                // The 'originalCategory' for learning is what it was before this edit.
                 transactionClassifier.learnFromUserCorrection(transactionToEdit, originalCategoryBeforeEdit, category);
            }
            
            // Clear the AI suggestion from the transaction object as it has been handled
            transactionToEdit.setTransientAiSuggestedCategory(null);

            // Persist changes through TransactionManager
            if (this.transactionManager.updateTransaction(this.transactionToEdit)) {
                confirmed = true;
                closeDialog();
            } else {
                // This case should ideally not happen if transactionToEdit came from the manager
                showAlert("保存错误", "无法在管理器中找到并更新该交易记录。");
                confirmed = false; // Ensure confirmed is false if update fails
            }
        } catch (NumberFormatException e) {
            showAlert("输入错误", "金额必须是有效的数字。");
        } catch (Exception e) {
            showAlert("保存错误", "保存交易时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancelAction(ActionEvent event) {
        closeDialog();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    private void closeDialog() {
        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.close();
    }

    private boolean validateInput() {
        String errorMessage = "";
        if (amountField.getText() == null || amountField.getText().isEmpty()) {
            errorMessage += "金额不能为空。\n";
        } else {
            try {
                Double.parseDouble(amountField.getText());
            } catch (NumberFormatException e) {
                errorMessage += "金额格式无效。\n";
            }
        }
        if (datePicker.getValue() == null) {
            errorMessage += "日期不能为空。\n";
        }
        if (categoryComboBox.getValue() == null || categoryComboBox.getValue().isEmpty()) {
            errorMessage += "类别不能为空。\n";
        }
        if (expenseToggleButton.isSelected() && (paymentMethodComboBox.getValue() == null || paymentMethodComboBox.getValue().isEmpty())) {
            errorMessage += "支付方式不能为空（支出时）。\n";
        }

        if (errorMessage.isEmpty()) {
            return true;
        } else {
            showAlert("输入验证失败", errorMessage);
            return false;
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR); // Use ERROR for validation issues
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
