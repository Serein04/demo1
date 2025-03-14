package com.financemanager.model;

import java.io.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 交易记录管理类
 * 负责交易记录的增删改查和持久化存储
 */
public class TransactionManager {
    private List<Transaction> transactions;
    private static final String DEFAULT_DATA_FILE = "data/transactions.csv";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    /**
     * 构造函数
     */
    public TransactionManager() {
        this.transactions = new ArrayList<>();
        loadTransactions(); // 初始化时尝试加载已有数据
    }
    
    /**
     * 添加交易记录
     */
    public void addTransaction(Transaction transaction) {
        transactions.add(transaction);
        saveTransactions(); // 保存到文件
    }
    
    /**
     * 删除交易记录
     */
    public boolean removeTransaction(String id) {
        boolean removed = transactions.removeIf(t -> t.getId().equals(id));
        if (removed) {
            saveTransactions(); // 保存到文件
        }
        return removed;
    }
    
    /**
     * 更新交易记录
     */
    public boolean updateTransaction(Transaction updatedTransaction) {
        for (int i = 0; i < transactions.size(); i++) {
            if (transactions.get(i).getId().equals(updatedTransaction.getId())) {
                transactions.set(i, updatedTransaction);
                saveTransactions(); // 保存到文件
                return true;
            }
        }
        return false;
    }
    
    /**
     * 获取所有交易记录
     */
    public List<Transaction> getAllTransactions() {
        return new ArrayList<>(transactions); // 返回副本以保护内部数据
    }
    
    /**
     * 按日期范围筛选交易
     */
    public List<Transaction> getTransactionsByDateRange(LocalDate startDate, LocalDate endDate) {
        return transactions.stream()
                .filter(t -> !t.getDate().isBefore(startDate) && !t.getDate().isAfter(endDate))
                .collect(Collectors.toList());
    }
    
    /**
     * 按类别筛选交易
     */
    public List<Transaction> getTransactionsByCategory(String category) {
        return transactions.stream()
                .filter(t -> t.getCategory().equalsIgnoreCase(category))
                .collect(Collectors.toList());
    }
    
    /**
     * 获取支出交易
     */
    public List<Transaction> getExpenseTransactions() {
        return transactions.stream()
                .filter(Transaction::isExpense)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取收入交易
     */
    public List<Transaction> getIncomeTransactions() {
        return transactions.stream()
                .filter(t -> !t.isExpense())
                .collect(Collectors.toList());
    }
    
    /**
     * 计算总支出
     */
    public double getTotalExpense() {
        return getExpenseTransactions().stream()
                .mapToDouble(Transaction::getAmount)
                .sum();
    }
    
    /**
     * 计算总收入
     */
    public double getTotalIncome() {
        return getIncomeTransactions().stream()
                .mapToDouble(Transaction::getAmount)
                .sum();
    }
    
    /**
     * 从CSV文件导入交易记录
     */
    public int importFromCSV(String filePath) {
        int importedCount = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            // 跳过标题行
            reader.readLine();
            while ((line = reader.readLine()) != null) {
                try {
                    String[] parts = line.split(",");
                    if (parts.length >= 6) {
                        double amount = Double.parseDouble(parts[0]);
                        LocalDate date = LocalDate.parse(parts[1], DATE_FORMATTER);
                        String category = parts[2];
                        String description = parts[3];
                        boolean isExpense = Boolean.parseBoolean(parts[4]);
                        String paymentMethod = parts[5];
                        
                        Transaction transaction = new Transaction(
                                amount, date, category, description, isExpense, paymentMethod);
                        transactions.add(transaction);
                        importedCount++;
                    }
                } catch (Exception e) {
                    System.err.println("导入行时出错: " + line + ", 错误: " + e.getMessage());
                }
            }
            if (importedCount > 0) {
                saveTransactions(); // 保存到文件
            }
        } catch (IOException e) {
            System.err.println("导入CSV文件时出错: " + e.getMessage());
        }
        return importedCount;
    }
    
    /**
     * 保存交易记录到CSV文件
     */
    private void saveTransactions() {
        File dataDir = new File("data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(DEFAULT_DATA_FILE))) {
            // 写入标题行
            writer.println("id,amount,date,category,description,isExpense,paymentMethod");
            
            // 写入数据行
            for (Transaction t : transactions) {
                writer.println(String.format("%s,%.2f,%s,%s,%s,%b,%s",
                        t.getId(),
                        t.getAmount(),
                        t.getDate().format(DATE_FORMATTER),
                        escapeCsv(t.getCategory()),
                        escapeCsv(t.getDescription()),
                        t.isExpense(),
                        escapeCsv(t.getPaymentMethod())));
            }
        } catch (IOException e) {
            System.err.println("保存交易记录时出错: " + e.getMessage());
        }
    }
    
    /**
     * 从CSV文件加载交易记录
     */
    private void loadTransactions() {
        File file = new File(DEFAULT_DATA_FILE);
        if (!file.exists()) {
            return; // 文件不存在，不加载
        }
        
        transactions.clear();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            // 跳过标题行
            reader.readLine();
            while ((line = reader.readLine()) != null) {
                try {
                    String[] parts = line.split(",");
                    if (parts.length >= 7) {
                        String id = parts[0];
                        double amount = Double.parseDouble(parts[1]);
                        LocalDate date = LocalDate.parse(parts[2], DATE_FORMATTER);
                        String category = parts[3];
                        String description = parts[4];
                        boolean isExpense = Boolean.parseBoolean(parts[5]);
                        String paymentMethod = parts[6];
                        
                        Transaction transaction = new Transaction(
                                id, amount, date, category, description, isExpense, paymentMethod);
                        transactions.add(transaction);
                    }
                } catch (Exception e) {
                    System.err.println("加载行时出错: " + line + ", 错误: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("加载交易记录时出错: " + e.getMessage());
        }
    }
    
    /**
     * 转义CSV字段中的特殊字符
     */
    private String escapeCsv(String field) {
        if (field == null) {
            return "";
        }
        // 如果字段包含逗号、引号或换行符，则用引号包围并转义内部引号
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }

    public double getCategoryExpenseTotal(String category) {
        double total = 0.0;
        for (Transaction transaction : transactions) {
            if (transaction.isExpense() && transaction.getCategory().equals(category)) {
                total += transaction.getAmount();
            }
        }
        return total;
    }



    public double getExpensesByCategory(String category, YearMonth yearMonth) {
        return transactions.stream()
                .filter(Transaction::isExpense)
                .filter(t -> t.getCategory().equals(category))
                .filter(t -> YearMonth.from(t.getDate()).equals(yearMonth))
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    /**
     * 计算当前月份的总支出
     * @return 当前月份的总支出金额
     */
    public double getCurrentMonthExpenses() {
        YearMonth currentMonth = YearMonth.now();
        return transactions.stream()
                .filter(Transaction::isExpense)
                .filter(t -> YearMonth.from(t.getDate()).equals(currentMonth))
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    /**
     * 计算当前月份的总储蓄金额（收入减去支出）
     * @return 当前月份的储蓄金额
     */
    public double getCurrentMonthSavings() {
        YearMonth currentMonth = YearMonth.now();
        double monthlyIncome = transactions.stream()
                .filter(t -> !t.isExpense())
                .filter(t -> YearMonth.from(t.getDate()).equals(currentMonth))
                .mapToDouble(Transaction::getAmount)
                .sum();
        
        double monthlyExpense = getCurrentMonthExpenses();
        return monthlyIncome - monthlyExpense;
    }
}