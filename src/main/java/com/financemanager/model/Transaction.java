package com.financemanager.model;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 交易记录类
 * 表示用户的一笔财务交易，包含金额、日期、类别、描述等信息
 */
public class Transaction {
    private String id;
    private double amount;
    private LocalDate date;
    private String category;
    private String description;
    private boolean isExpense; // true表示支出，false表示收入
    private String paymentMethod; // 支付方式（现金、信用卡、微信、支付宝等）
    
    /**
     * 构造函数
     */
    public Transaction(double amount, LocalDate date, String category, String description, boolean isExpense, String paymentMethod) {
        if (date == null) {
            throw new IllegalArgumentException("日期不能为空");
        }
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("类别不能为空");
        }
        if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
            throw new IllegalArgumentException("支付方式不能为空");
        }
        
        this.id = UUID.randomUUID().toString(); // 生成唯一ID
        this.amount = amount;
        this.date = date;
        this.category = category;
        this.description = description != null ? description : "";
        this.isExpense = isExpense;
        this.paymentMethod = paymentMethod;
    }
    
    /**
     * 用于从存储数据恢复的构造函数
     */
    public Transaction(String id, double amount, LocalDate date, String category, String description, boolean isExpense, String paymentMethod) {
        this.id = id;
        this.amount = amount;
        this.date = date;
        this.category = category;
        this.description = description;
        this.isExpense = isExpense;
        this.paymentMethod = paymentMethod;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isExpense() {
        return isExpense;
    }

    public void setExpense(boolean expense) {
        isExpense = expense;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    
    @Override
    public String toString() {
        return "Transaction{" +
                "id='" + id + '\'' +
                ", amount=" + amount +
                ", date=" + date +
                ", category='" + category + '\'' +
                ", description='" + description + '\'' +
                ", isExpense=" + isExpense +
                ", paymentMethod='" + paymentMethod + '\'' +
                '}';
    }
}