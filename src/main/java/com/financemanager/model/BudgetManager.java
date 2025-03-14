package com.financemanager.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * 预算管理类
 * 负责管理用户的预算目标和储蓄计划
 */
public class BudgetManager {
    private final Map<String, Double> categoryBudgets; // 各类别的预算限额
    private double monthlyBudget; // 月度总预算
    private double savingsGoal; // 储蓄目标
    private static final String BUDGET_FILE = "data/budget.csv";
    
    public BudgetManager() {
        this.categoryBudgets = new HashMap<>();
        this.monthlyBudget = 0.0;
        this.savingsGoal = 0.0;
        loadBudgetData();
    }
    
    /**
     * 设置月度总预算
     */
    public void setMonthlyBudget(double amount) {
        this.monthlyBudget = amount;
        saveBudgetData();
    }
    
    /**
     * 设置储蓄目标
     */
    public void setSavingsGoal(double amount) {
        this.savingsGoal = amount;
        saveBudgetData();
    }
    
    /**
     * 设置类别预算
     */
    public void setCategoryBudget(String category, double amount) {
        categoryBudgets.put(category, amount);
        saveBudgetData();
    }
    
    /**
     * 获取月度总预算
     */
    public double getMonthlyBudget() {
        return monthlyBudget;
    }
    
    /**
     * 获取储蓄目标
     */
    public double getSavingsGoal() {
        return savingsGoal;
    }
    
    /**
     * 获取类别预算
     */
    public double getCategoryBudget(String category) {
        return categoryBudgets.getOrDefault(category, 0.0);
    }
    
    /**
     * 获取所有类别预算
     */
    public Map<String, Double> getAllCategoryBudgets() {
        return new HashMap<>(categoryBudgets);
    }
    
    /**
     * 检查类别支出是否超出预算
     */
    public boolean isCategoryOverBudget(String category, double currentSpending) {
        double budget = getCategoryBudget(category);
        return budget > 0 && currentSpending > budget;
    }
    
    /**
     * 计算月度预算剩余金额
     */
    public double getRemainingMonthlyBudget(double currentSpending) {
        return monthlyBudget - currentSpending;
    }
    
    /**
     * 计算储蓄目标完成度
     */
    public double getSavingsProgress(double currentSavings) {
        return savingsGoal > 0 ? (currentSavings / savingsGoal) * 100 : 0;
    }
    
    /**
     * 计算所有类别预算的总和
     */
    public double getTotalCategoryBudget() {
        double total = 0.0;
        for (Double budget : categoryBudgets.values()) {
            total += budget;
        }
        return total;
    }
    
    /**
     * 保存预算数据到文件
     */
    private void saveBudgetData() {
        File dataDir = new File("data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(BUDGET_FILE))) {
            // 写入月度预算和储蓄目标
            writer.println(String.format("monthly,%.2f", monthlyBudget));
            writer.println(String.format("savings,%.2f", savingsGoal));
            
            // 写入类别预算
            for (Map.Entry<String, Double> entry : categoryBudgets.entrySet()) {
                writer.println(String.format("category,%s,%.2f",
                        entry.getKey(), entry.getValue()));
            }
        } catch (IOException e) {
            System.err.println("保存预算数据时出错: " + e.getMessage());
        }
    }
    
    /**
     * 从文件加载预算数据
     */
    private void loadBudgetData() {
        File file = new File(BUDGET_FILE);
        if (!file.exists()) {
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 2) {
                    switch (parts[0]) {
                        case "monthly":
                            monthlyBudget = Double.parseDouble(parts[1]);
                            break;
                        case "savings":
                            savingsGoal = Double.parseDouble(parts[1]);
                            break;
                        case "category":
                            if (parts.length >= 3) {
                                categoryBudgets.put(parts[1], Double.valueOf(parts[2]));
                            }
                            break;
                    }
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("加载预算数据时出错: " + e.getMessage());
        }
    }
}
