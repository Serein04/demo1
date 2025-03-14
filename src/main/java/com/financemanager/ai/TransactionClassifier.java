package com.financemanager.ai;

import com.financemanager.model.Transaction;
import java.util.*;
import java.io.*;

/**
 * 交易分类器类
 * 负责使用AI技术对交易进行自动分类，并允许用户手动校正
 */
public class TransactionClassifier {
    // 预定义的交易类别
    private static final List<String> DEFAULT_EXPENSE_CATEGORIES = Arrays.asList(
            "餐饮", "购物", "交通", "住房", "娱乐", "教育", "医疗", "旅行", 
            "日用品", "通讯", "服装", "礼品", "其他支出"
    );
    
    private static final List<String> DEFAULT_INCOME_CATEGORIES = Arrays.asList(
            "工资", "奖金", "投资收益", "兼职收入", "礼金", "退款", "其他收入"
    );
    
    // 用户自定义的类别关键词映射
    private Map<String, List<String>> categoryKeywords;
    private static final String KEYWORDS_FILE = "data/category_keywords.csv";
    
    public TransactionClassifier() {
        this.categoryKeywords = new HashMap<>();
        loadDefaultKeywords();
        loadUserKeywords();
    }
    
    /**
     * 加载默认的关键词映射
     */
    private void loadDefaultKeywords() {
        // 餐饮类关键词
        categoryKeywords.put("餐饮", Arrays.asList(
                "餐厅", "饭店", "食堂", "外卖", "美食", "小吃", "咖啡", "奶茶", 
                "早餐", "午餐", "晚餐", "宵夜", "火锅", "烧烤", "快餐"
        ));
        
        // 购物类关键词
        categoryKeywords.put("购物", Arrays.asList(
                "超市", "商场", "淘宝", "京东", "拼多多", "电商", "网购", 
                "购物中心", "百货", "便利店", "市场"
        ));
        
        // 交通类关键词
        categoryKeywords.put("交通", Arrays.asList(
                "地铁", "公交", "出租车", "打车", "滴滴", "高铁", "火车", "飞机", 
                "机票", "加油", "停车费", "过路费", "共享单车"
        ));
        
        // 其他类别的默认关键词...
        // 实际应用中可以添加更多类别的关键词
    }
    
    /**
     * 从文件加载用户自定义的关键词映射
     */
    private void loadUserKeywords() {
        File file = new File(KEYWORDS_FILE);
        if (!file.exists()) {
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            // 跳过标题行
            reader.readLine();
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", 2);
                if (parts.length >= 2) {
                    String category = parts[0];
                    String[] keywords = parts[1].split(",");
                    
                    List<String> keywordList = categoryKeywords.getOrDefault(category, new ArrayList<>());
                    keywordList.addAll(Arrays.asList(keywords));
                    categoryKeywords.put(category, keywordList);
                }
            }
        } catch (IOException e) {
            System.err.println("加载类别关键词时出错: " + e.getMessage());
        }
    }
    
    /**
     * 保存用户自定义的关键词映射到文件
     */
    public void saveUserKeywords() {
        File dataDir = new File("data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(KEYWORDS_FILE))) {
            // 写入标题行
            writer.println("category,keywords");
            
            // 写入数据行
            for (Map.Entry<String, List<String>> entry : categoryKeywords.entrySet()) {
                String category = entry.getKey();
                List<String> keywords = entry.getValue();
                
                if (!keywords.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(category).append(",");
                    
                    for (int i = 0; i < keywords.size(); i++) {
                        sb.append(keywords.get(i));
                        if (i < keywords.size() - 1) {
                            sb.append(",");
                        }
                    }
                    
                    writer.println(sb.toString());
                }
            }
        } catch (IOException e) {
            System.err.println("保存类别关键词时出错: " + e.getMessage());
        }
    }
    
    /**
     * 添加用户自定义的关键词
     */
    public void addCategoryKeyword(String category, String keyword) {
        List<String> keywords = categoryKeywords.getOrDefault(category, new ArrayList<>());
        if (!keywords.contains(keyword)) {
            keywords.add(keyword);
            categoryKeywords.put(category, keywords);
            saveUserKeywords();
        }
    }
    
    /**
     * 移除用户自定义的关键词
     */
    public boolean removeCategoryKeyword(String category, String keyword) {
        List<String> keywords = categoryKeywords.get(category);
        if (keywords != null && keywords.remove(keyword)) {
            saveUserKeywords();
            return true;
        }
        return false;
    }
    
    /**
     * 获取所有支出类别
     */
    public List<String> getExpenseCategories() {
        return new ArrayList<>(DEFAULT_EXPENSE_CATEGORIES);
    }
    
    /**
     * 获取所有收入类别
     */
    public List<String> getIncomeCategories() {
        return new ArrayList<>(DEFAULT_INCOME_CATEGORIES);
    }
    
    /**
     * 基于关键词匹配对交易进行分类
     * 这是一个简单的基于规则的分类方法，实际应用中可以替换为更复杂的AI算法
     */
    public String classifyTransaction(Transaction transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("交易记录不能为空");
        }
        
        String description = transaction.getDescription();
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("交易描述不能为空，请提供有效的描述信息以便进行分类");
        }
        
        description = description.trim().toLowerCase();
        boolean isExpense = transaction.isExpense();
        
        // 根据交易描述中的关键词进行分类
        for (Map.Entry<String, List<String>> entry : categoryKeywords.entrySet()) {
            String category = entry.getKey();
            List<String> keywords = entry.getValue();
            
            if (keywords != null && !keywords.isEmpty()) {
                for (String keyword : keywords) {
                    if (keyword != null && !keyword.trim().isEmpty() && 
                        description.contains(keyword.toLowerCase().trim())) {
                        // 检查类别是否与交易类型匹配（支出/收入）
                        if ((isExpense && DEFAULT_EXPENSE_CATEGORIES.contains(category)) ||
                            (!isExpense && DEFAULT_INCOME_CATEGORIES.contains(category))) {
                            return category;
                        }
                    }
                }
            }
        }
        
        // 如果没有匹配的关键词，返回默认类别
        return isExpense ? "其他支出" : "其他收入";
    }
    
    /**
     * 批量分类交易
     */
    public Map<String, String> batchClassifyTransactions(List<Transaction> transactions) {
        Map<String, String> results = new HashMap<>();
        for (Transaction transaction : transactions) {
            String category = classifyTransaction(transaction);
            results.put(transaction.getId(), category);
        }
        return results;
    }
    
    /**
     * 学习用户的分类修正
     * 当用户手动修改分类时，系统可以学习这种修正，提高未来分类的准确性
     */
    public void learnFromUserCorrection(Transaction transaction, String originalCategory, String correctedCategory) {
        // 如果用户修改了分类，将交易描述中的关键词添加到正确类别的关键词列表中
        if (!originalCategory.equals(correctedCategory)) {
            String description = transaction.getDescription();
            // 提取可能的关键词（这里简化为使用整个描述作为关键词）
            addCategoryKeyword(correctedCategory, description);
        }
    }
}