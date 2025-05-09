package com.financemanager.ai;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.financemanager.model.Transaction;

/**
 * 支出分析器类
 * 负责分析用户的消费习惯，提供支出洞察和预算建议
 */
public class ExpenseAnalyzer {
    private static final int MONTHS_TO_ANALYZE = 6; // 分析最近6个月的数据
    private static final double SEASONAL_THRESHOLD = 1.5; // 季节性支出阈值（相对于平均值）
    
    private final TransactionClassifier classifier;
    
    /**
     * 构造函数
     */
    public ExpenseAnalyzer(TransactionClassifier classifier) {
        this.classifier = classifier;
    }
    
    /**
     * 获取基本统计数据
     * @param transactions 交易记录列表
     * @return 基本统计信息
     */
    public String getBasicStatistics(List<Transaction> transactions) {
        if (transactions.isEmpty()) {
            return "暂无交易数据可供分析。";
        }
        
        // 计算总收入和总支出
        double totalIncome = transactions.stream()
                .filter(t -> !t.isExpense())
                .mapToDouble(Transaction::getAmount)
                .sum();
                
        double totalExpense = transactions.stream()
                .filter(Transaction::isExpense)
                .mapToDouble(Transaction::getAmount)
                .sum();
        
        // 获取月度支出趋势
        Map<YearMonth, Double> monthlyTrend = analyzeMonthlyTrend(transactions);
        
        // 获取支出类别分布
        Map<String, Double> categoryDistribution = analyzeCategoryDistribution(transactions);
        
        // 检测异常支出
        List<Transaction> abnormalExpenses = detectAbnormalExpenses(transactions);
        
        // 构建统计信息字符串
        StringBuilder stats = new StringBuilder();
        stats.append(String.format("总收入：%.2f\n", totalIncome));
        stats.append(String.format("总支出：%.2f\n", totalExpense));
        stats.append(String.format("结余：%.2f\n\n", totalIncome - totalExpense));
        
        // 添加月度趋势信息
        stats.append("月度支出趋势：\n");
        monthlyTrend.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> stats.append(String.format("%s: %.2f\n", 
                        entry.getKey().toString(), entry.getValue())));
        stats.append("\n");
        
        // 添加类别分布信息
        stats.append("支出类别分布：\n");
        categoryDistribution.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                .forEach(entry -> stats.append(String.format("%s: %.1f%%\n", 
                        entry.getKey(), entry.getValue())));
        stats.append("\n");
        
        // 添加异常支出信息
        if (!abnormalExpenses.isEmpty()) {
            stats.append("检测到的异常支出：\n");
            abnormalExpenses.forEach(t -> stats.append(String.format("%s: %.2f (%s)\n", 
                    t.getDate().toString(), t.getAmount(), t.getCategory())));
        }
        
        return stats.toString();
    }
    
    /**
     * 分析月度支出趋势
     * @param transactions 交易记录列表
     * @return 月度支出趋势分析结果
     */
    public Map<YearMonth, Double> analyzeMonthlyTrend(List<Transaction> transactions) {
        // 按月份分组并计算每月总支出
        Map<YearMonth, Double> monthlyExpenses = transactions.stream()
                .filter(Transaction::isExpense)
                .collect(Collectors.groupingBy(
                        t -> YearMonth.from(t.getDate()),
                        Collectors.summingDouble(Transaction::getAmount)
                ));
        
        return monthlyExpenses;
    }
    
    /**
     * 分析类别支出分布
     * @param transactions 交易记录列表
     * @return 各类别支出占比
     */
    public Map<String, Double> analyzeCategoryDistribution(List<Transaction> transactions) {
        // 计算总支出
        double totalExpense = transactions.stream()
                .filter(Transaction::isExpense)
                .mapToDouble(Transaction::getAmount)
                .sum();
        
        if (totalExpense == 0) {
            return new HashMap<>();
        }
        
        // 按类别分组并计算占比
        Map<String, Double> categoryPercentages = new HashMap<>();
        Map<String, Double> categoryAmounts = transactions.stream()
                .filter(Transaction::isExpense)
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        Collectors.summingDouble(Transaction::getAmount)
                ));
        
        for (Map.Entry<String, Double> entry : categoryAmounts.entrySet()) {
            categoryPercentages.put(entry.getKey(), (entry.getValue() / totalExpense) * 100);
        }
        
        return categoryPercentages;
    }
    
    /**
     * 检测异常支出
     * @param transactions 交易记录列表
     * @return 异常支出交易列表
     */
    public List<Transaction> detectAbnormalExpenses(List<Transaction> transactions) {
        // 按类别分组计算平均支出
        Map<String, Double> categoryAverages = new HashMap<>();
        Map<String, List<Transaction>> categoryTransactions = transactions.stream()
                .filter(Transaction::isExpense)
                .collect(Collectors.groupingBy(Transaction::getCategory));
        
        for (Map.Entry<String, List<Transaction>> entry : categoryTransactions.entrySet()) {
            String category = entry.getKey();
            List<Transaction> categoryTxs = entry.getValue();
            double average = categoryTxs.stream()
                    .mapToDouble(Transaction::getAmount)
                    .average()
                    .orElse(0);
            categoryAverages.put(category, average);
        }
        
        // 检测异常支出（超过类别平均值的3倍）
        return transactions.stream()
                .filter(Transaction::isExpense)
                .filter(t -> {
                    double average = categoryAverages.getOrDefault(t.getCategory(), 0.0);
                    return average > 0 && t.getAmount() > average * 3;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 检测季节性支出模式
     * @param transactions 交易记录列表
     * @return 季节性支出模式分析结果
     */
    public Map<Month, List<String>> detectSeasonalPatterns(List<Transaction> transactions) {
        // 按月份和类别分组计算支出
        Map<Month, Map<String, Double>> monthCategoryExpenses = new HashMap<>();
        for (Month month : Month.values()) {
            monthCategoryExpenses.put(month, new HashMap<>());
        }
        
        // 计算每个月每个类别的总支出
        for (Transaction t : transactions) {
            if (t.isExpense()) {
                Month month = t.getDate().getMonth();
                String category = t.getCategory();
                Map<String, Double> categoryExpenses = monthCategoryExpenses.get(month);
                categoryExpenses.put(category, categoryExpenses.getOrDefault(category, 0.0) + t.getAmount());
            }
        }
        
        // 计算每个类别的月平均支出
        Map<String, Double> categoryMonthlyAverages = new HashMap<>();
        Set<String> allCategories = new HashSet<>();
        
        for (Map<String, Double> categoryExpenses : monthCategoryExpenses.values()) {
            allCategories.addAll(categoryExpenses.keySet());
        }
        
        for (String category : allCategories) {
            double totalExpense = 0;
            int monthCount = 0;
            for (Map<String, Double> categoryExpenses : monthCategoryExpenses.values()) {
                if (categoryExpenses.containsKey(category)) {
                    totalExpense += categoryExpenses.get(category);
                    monthCount++;
                }
            }
            if (monthCount > 0) {
                categoryMonthlyAverages.put(category, totalExpense / monthCount);
            }
        }
        
        // 检测季节性支出（某月某类别支出显著高于平均值）
        Map<Month, List<String>> seasonalPatterns = new HashMap<>();
        for (Month month : Month.values()) {
            List<String> seasonalCategories = new ArrayList<>();
            Map<String, Double> categoryExpenses = monthCategoryExpenses.get(month);
            
            for (Map.Entry<String, Double> entry : categoryExpenses.entrySet()) {
                String category = entry.getKey();
                double expense = entry.getValue();
                double average = categoryMonthlyAverages.getOrDefault(category, 0.0);
                
                if (average > 0 && expense > average * SEASONAL_THRESHOLD) {
                    seasonalCategories.add(category);
                }
            }
            
            if (!seasonalCategories.isEmpty()) {
                seasonalPatterns.put(month, seasonalCategories);
            }
        }
        
        return seasonalPatterns;
    }
    
    /**
     * 分析节省机会
     * @param transactions 交易记录列表
     * @return 可能的节省机会
     */
    public List<Map<String, Object>> analyzeSavingOpportunities(List<Transaction> transactions) {
        List<Map<String, Object>> opportunities = new ArrayList<>();
        
        // 检测频繁的小额支出
        Map<String, List<Transaction>> frequentSmallExpenses = findFrequentSmallExpenses(transactions);
        for (Map.Entry<String, List<Transaction>> entry : frequentSmallExpenses.entrySet()) {
            String category = entry.getKey();
            List<Transaction> txs = entry.getValue();
            double totalAmount = txs.stream().mapToDouble(Transaction::getAmount).sum();
            
            Map<String, Object> opportunity = new HashMap<>();
            opportunity.put("type", "frequentSmall");
            opportunity.put("category", category);
            opportunity.put("count", txs.size());
            opportunity.put("totalAmount", totalAmount);
            opportunity.put("description", "频繁的小额" + category + "支出累计达到" + String.format("%.2f", totalAmount) + "元");
            
            opportunities.add(opportunity);
        }
        
        // 检测可替代的高价支出
        List<Transaction> highPriceExpenses = findHighPriceExpenses(transactions);
        for (Transaction t : highPriceExpenses) {
            Map<String, Object> opportunity = new HashMap<>();
            opportunity.put("type", "highPrice");
            opportunity.put("category", t.getCategory());
            opportunity.put("amount", t.getAmount());
            opportunity.put("date", t.getDate());
            opportunity.put("description", "在" + t.getCategory() + "类别中有一笔" + String.format("%.2f", t.getAmount()) + "元的大额支出");
            
            opportunities.add(opportunity);
        }
        
        return opportunities;
    }
    
    /**
     * 根据类别提供详细分析
     * @param transactions 交易记录列表
     * @param category 类别名称
     * @return 详细分析结果
     */
    public Map<String, Object> analyzeCategoryDetails(List<Transaction> transactions, String category) {
        Map<String, Object> result = new HashMap<>();
        
        // 筛选指定类别的交易
        List<Transaction> categoryTransactions = transactions.stream()
                .filter(t -> t.getCategory().equals(category))
                .collect(Collectors.toList());
        
        if (categoryTransactions.isEmpty()) {
            result.put("error", "没有找到该类别的交易记录");
            return result;
        }
        
        // 获取类别描述
        String description = classifier.getCategoryDescription(category);
        result.put("description", description);
        
        // 计算基本统计数据
        double totalAmount = categoryTransactions.stream()
                .mapToDouble(Transaction::getAmount)
                .sum();
        double averageAmount = categoryTransactions.stream()
                .mapToDouble(Transaction::getAmount)
                .average()
                .orElse(0);
        int count = categoryTransactions.size();
        
        result.put("totalAmount", totalAmount);
        result.put("averageAmount", averageAmount);
        result.put("count", count);
        
        // 分析月度趋势
        Map<YearMonth, Double> monthlyTrend = categoryTransactions.stream()
                .collect(Collectors.groupingBy(
                        t -> YearMonth.from(t.getDate()),
                        Collectors.summingDouble(Transaction::getAmount)
                ));
        result.put("monthlyTrend", monthlyTrend);
        
        // 分析相关类别
        Map<String, List<String>> relatedCategories = classifier.analyzeRelatedCategories(transactions);
        if (relatedCategories.containsKey(category)) {
            result.put("relatedCategories", relatedCategories.get(category));
        }
        
        // 检测异常支出
        double categoryAverage = averageAmount;
        List<Transaction> abnormalExpenses = categoryTransactions.stream()
                .filter(t -> t.getAmount() > categoryAverage * 2)
                .collect(Collectors.toList());
        result.put("abnormalExpenses", abnormalExpenses);
        
        // 分析支出频率
        Map<DayOfWeek, Long> dayOfWeekFrequency = categoryTransactions.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getDate().getDayOfWeek(),
                        Collectors.counting()
                ));
        result.put("dayOfWeekFrequency", dayOfWeekFrequency);
        
        return result;
    }
    
    /**
     * 查找频繁的小额支出
     */
    private Map<String, List<Transaction>> findFrequentSmallExpenses(List<Transaction> transactions) {
        // 获取最近一个月的交易
        LocalDate oneMonthAgo = LocalDate.now().minusMonths(1);
        List<Transaction> recentTransactions = transactions.stream()
                .filter(Transaction::isExpense)
                .filter(t -> !t.getDate().isBefore(oneMonthAgo))
                .collect(Collectors.toList());
        
        // 按类别分组
        Map<String, List<Transaction>> categoryTransactions = recentTransactions.stream()
                .collect(Collectors.groupingBy(Transaction::getCategory));
        
        // 筛选出频繁的小额支出（单笔金额较小但频次较高的类别）
        Map<String, List<Transaction>> frequentSmallExpenses = new HashMap<>();
        for (Map.Entry<String, List<Transaction>> entry : categoryTransactions.entrySet()) {
            String category = entry.getKey();
            List<Transaction> txs = entry.getValue();
            
            // 计算该类别的平均交易金额
            double averageAmount = txs.stream()
                    .mapToDouble(Transaction::getAmount)
                    .average()
                    .orElse(0);
            
            // 如果平均金额较小（小于100元）且交易次数较多（大于5次），认为是频繁小额支出
            if (averageAmount < 100 && txs.size() > 5) {
                frequentSmallExpenses.put(category, txs);
            }
        }
        
        return frequentSmallExpenses;
    }
    
    /**
     * 查找高价支出
     */
    private List<Transaction> findHighPriceExpenses(List<Transaction> transactions) {
        // 获取最近三个月的交易
        LocalDate threeMonthsAgo = LocalDate.now().minusMonths(3);
        List<Transaction> recentTransactions = transactions.stream()
                .filter(Transaction::isExpense)
                .filter(t -> !t.getDate().isBefore(threeMonthsAgo))
                .collect(Collectors.toList());
        
        // 计算总体平均支出
        double overallAverage = recentTransactions.stream()
                .mapToDouble(Transaction::getAmount)
                .average()
                .orElse(0);
        
        // 筛选出金额显著高于平均值的支出（超过平均值的3倍且大于500元）
        return recentTransactions.stream()
                .filter(t -> t.getAmount() > overallAverage * 3 && t.getAmount() > 500)
                .collect(Collectors.toList());
    }
}
