package com.financemanager.ai;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.financemanager.model.Transaction;

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
    
    // 类别描述，用于AI分析
    private static final Map<String, String> CATEGORY_DESCRIPTIONS = new HashMap<>();
    static {
        // 支出类别描述
        CATEGORY_DESCRIPTIONS.put("餐饮", "包括在餐厅、咖啡店、外卖等食品消费");
        CATEGORY_DESCRIPTIONS.put("购物", "包括在超市、商场、网购平台等购买商品");
        CATEGORY_DESCRIPTIONS.put("交通", "包括公共交通、打车、加油、停车费等");
        CATEGORY_DESCRIPTIONS.put("住房", "包括房租、物业费、水电费等住房相关支出");
        CATEGORY_DESCRIPTIONS.put("娱乐", "包括电影、游戏、KTV等娱乐活动");
        CATEGORY_DESCRIPTIONS.put("教育", "包括学费、书籍、培训课程等教育支出");
        CATEGORY_DESCRIPTIONS.put("医疗", "包括看病、买药、体检等医疗支出");
        CATEGORY_DESCRIPTIONS.put("旅行", "包括机票、酒店、景点门票等旅行支出");
        CATEGORY_DESCRIPTIONS.put("日用品", "包括洗漱用品、清洁用品等日常必需品");
        CATEGORY_DESCRIPTIONS.put("通讯", "包括手机话费、网费等通讯支出");
        CATEGORY_DESCRIPTIONS.put("服装", "包括衣服、鞋子、配饰等服装支出");
        CATEGORY_DESCRIPTIONS.put("礼品", "包括送礼、红包等礼品支出");
        CATEGORY_DESCRIPTIONS.put("其他支出", "不属于以上类别的其他支出");
        
        // 收入类别描述
        CATEGORY_DESCRIPTIONS.put("工资", "包括固定工资、奖金、津贴等");
        CATEGORY_DESCRIPTIONS.put("奖金", "包括年终奖、绩效奖金、项目奖金等");
        CATEGORY_DESCRIPTIONS.put("投资收益", "包括股票、基金、理财产品等投资收益");
        CATEGORY_DESCRIPTIONS.put("兼职收入", "包括兼职工作、自由职业等收入");
        CATEGORY_DESCRIPTIONS.put("礼金", "包括收到的红包、礼金等");
        CATEGORY_DESCRIPTIONS.put("退款", "包括商品退款、服务退款等");
        CATEGORY_DESCRIPTIONS.put("其他收入", "不属于以上类别的其他收入");
    }
    
    // 用户自定义的类别关键词映射
    private final Map<String, List<String>> categoryKeywords;
    private static final String KEYWORDS_FILE = "data/category_keywords.csv";
    private AIService aiService; // Added AIService
    
    // Constructor updated to accept AIService
    public TransactionClassifier(AIService aiService) {
        this.aiService = aiService;
        this.categoryKeywords = new HashMap<>();
        loadDefaultKeywords();
        loadUserKeywords();
    }

    // Overloaded constructor for backward compatibility or keyword-only mode
    public TransactionClassifier() {
        this(null); // Calls the main constructor with null AIService
    }
    
    /**
     * 加载默认的关键词映射
     */
    private void loadDefaultKeywords() {
        // 餐饮类关键词
        List<String> foodKeywords = new ArrayList<>(Arrays.asList(
                "餐厅", "饭店", "食堂", "外卖", "美食", "小吃", "咖啡", "奶茶", 
                "早餐", "午餐", "晚餐", "宵夜", "火锅", "烧烤", "快餐"
        ));
        categoryKeywords.put("餐饮", foodKeywords);
        
        // 购物类关键词
        List<String> shoppingKeywords = new ArrayList<>(Arrays.asList(
                "超市", "商场", "淘宝", "京东", "拼多多", "电商", "网购", 
                "购物中心", "百货", "便利店", "市场"
        ));
        categoryKeywords.put("购物", shoppingKeywords);
        
        // 交通类关键词
        List<String> transportKeywords = new ArrayList<>(Arrays.asList(
                "地铁", "公交", "出租车", "打车", "滴滴", "高铁", "火车", "飞机", 
                "机票", "加油", "停车费", "过路费", "共享单车"
        ));
        categoryKeywords.put("交通", transportKeywords);
        
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
     * Main method to classify a transaction.
     * It first tries to use LLM if AIService is available and configured.
     * If LLM classification fails or is not available, it falls back to keyword-based classification.
     */
    public String classifyTransaction(Transaction transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("交易记录不能为空");
        }
        String description = transaction.getDescription();
        if (description == null || description.trim().isEmpty()) {
            // For LLM, a description is crucial. If keyword-based can handle it, it will.
            // Otherwise, let keyword-based throw the specific error or return default.
             return classifyTransactionKeywordBased(transaction);
        }

        if (this.aiService != null) {
            try {
                String prompt = buildPromptForLLM(transaction);
                String llmResponse = aiService.getLLMChatCompletion(prompt);
                String chosenCategory = llmResponse.trim();

                List<String> validCategories = transaction.isExpense() ? getExpenseCategories() : getIncomeCategories();
                if (validCategories.contains(chosenCategory)) {
                    System.out.println("LLM classified transaction '" + transaction.getDescription() + "' as: " + chosenCategory);
                    return chosenCategory;
                } else {
                    System.err.println("LLM返回无效类别: " + chosenCategory + " for transaction '" + transaction.getDescription() + "'. 回退到关键词分类。");
                }
            } catch (IOException | InterruptedException e) {
                System.err.println("调用LLM API进行分类时出错: " + e.getMessage() + " for transaction '" + transaction.getDescription() + "'. 回退到关键词分类。");
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt(); // Restore interruption status
                }
            } catch (Exception e) { // Catch any other unexpected errors from AI service
                 System.err.println("LLM分类时发生未知错误: " + e.getMessage() + " for transaction '" + transaction.getDescription() + "'. 回退到关键词分类。");
            }
        } else {
            System.out.println("AIService未配置，使用关键词分类 for transaction '" + transaction.getDescription() + "'.");
        }
        
        // Fallback to keyword-based classification
        return classifyTransactionKeywordBased(transaction);
    }

    private String buildPromptForLLM(Transaction transaction) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("您是一个专业的财务助手。您的任务是根据交易描述和类型（支出或收入），将交易分类到预定义的类别之一。\n\n");
        promptBuilder.append("交易描述: \"").append(transaction.getDescription().replace("\"", "'")).append("\"\n"); // Escape quotes in description
        String transactionType = transaction.isExpense() ? "支出" : "收入";
        promptBuilder.append("交易类型: ").append(transactionType).append("\n\n");
        promptBuilder.append("请从以下类别列表中选择最合适的类别。\n");
        promptBuilder.append("请仅返回类别名称，不要添加任何其他文字或解释。\n\n");
        promptBuilder.append("可用的").append(transactionType).append("类别及其描述:\n{\n");
    
        List<String> categories = transaction.isExpense() ? getExpenseCategories() : getIncomeCategories();
        for (int i = 0; i < categories.size(); i++) {
            String category = categories.get(i);
            String categoryDesc = getCategoryDescription(category).replace("\"", "'"); // Escape quotes in description
            promptBuilder.append("  \"").append(category).append("\": \"").append(categoryDesc).append("\"");
            if (i < categories.size() - 1) {
                promptBuilder.append(",\n");
            } else {
                promptBuilder.append("\n");
            }
        }
        promptBuilder.append("}\n\n选择的类别:");
        return promptBuilder.toString();
    }

    /**
     * 基于关键词匹配对交易进行分类
     * 这是一个简单的基于规则的分类方法
     */
    public String classifyTransactionKeywordBased(Transaction transaction) {
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
     * This method will now use the primary classifyTransaction method, which may use LLM or keyword.
     */
    public Map<String, String> batchClassifyTransactions(List<Transaction> transactions) {
        Map<String, String> results = new HashMap<>();
        if (transactions == null) return results; // Handle null list
        for (Transaction transaction : transactions) {
            if (transaction != null && transaction.getId() != null) { // Ensure transaction and ID are not null
                 String category = classifyTransaction(transaction); // Uses the new primary classification method
                 results.put(transaction.getId(), category);
            }
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
    
    /**
     * 获取类别描述
     * 用于AI分析和用户理解
     */
    public String getCategoryDescription(String category) {
        return CATEGORY_DESCRIPTIONS.getOrDefault(category, "未提供描述");
    }
    
    /**
     * 获取所有类别及其描述
     */
    public Map<String, String> getAllCategoryDescriptions() {
        return new HashMap<>(CATEGORY_DESCRIPTIONS);
    }
    
    /**
     * 根据交易类别分组交易记录
     * 用于分析报告
     */
    public Map<String, List<Transaction>> groupTransactionsByCategory(List<Transaction> transactions) {
        Map<String, List<Transaction>> result = new HashMap<>();
        
        for (Transaction transaction : transactions) {
            String category = transaction.getCategory();
            if (!result.containsKey(category)) {
                result.put(category, new ArrayList<>());
            }
            result.get(category).add(transaction);
        }
        
        return result;
    }
    
    /**
     * 分析类别之间的关联性
     * 例如，某些类别的支出是否经常同时发生
     */
    public Map<String, List<String>> analyzeRelatedCategories(List<Transaction> transactions) {
        Map<String, List<String>> result = new HashMap<>();
        
        // 按日期分组交易
        Map<LocalDate, List<Transaction>> transactionsByDate = new HashMap<>();
        for (Transaction transaction : transactions) {
            LocalDate date = transaction.getDate();
            if (!transactionsByDate.containsKey(date)) {
                transactionsByDate.put(date, new ArrayList<>());
            }
            transactionsByDate.get(date).add(transaction);
        }
        
        // 分析同一天发生的不同类别交易
        for (List<Transaction> dailyTransactions : transactionsByDate.values()) {
            if (dailyTransactions.size() > 1) {
                for (Transaction t1 : dailyTransactions) {
                    String category1 = t1.getCategory();
                    if (!result.containsKey(category1)) {
                        result.put(category1, new ArrayList<>());
                    }
                    
                    for (Transaction t2 : dailyTransactions) {
                        String category2 = t2.getCategory();
                        if (!category1.equals(category2) && !result.get(category1).contains(category2)) {
                            result.get(category1).add(category2);
                        }
                    }
                }
            }
        }
        
        return result;
    }
}
