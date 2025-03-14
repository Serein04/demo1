package com.financemanager.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

import com.financemanager.ai.ExpenseAnalyzer;
import com.financemanager.model.Transaction;
import com.financemanager.model.TransactionManager;

/**
 * AI助手面板类
 * 提供与AI助手的聊天交互界面
 */
public class AIAssistantPanel extends JPanel {
    private final TransactionManager transactionManager;
    private final ExpenseAnalyzer analyzer;
    
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton;
    
    // API相关配置
    private static final String CONFIG_FILE = "config.properties";
    private static final String API_ENDPOINT = "https://api.siliconflow.cn/v1/chat/completions";
    private String apiKey;
    private final HttpClient httpClient;
    
    // 当前AI助手回复的ID，用于流式输出时追加内容
    private String currentAIResponseId = null;
    
    /**
     * 构造函数
     */
    public AIAssistantPanel(TransactionManager transactionManager, ExpenseAnalyzer analyzer) {
        this.transactionManager = transactionManager;
        this.analyzer = analyzer;
        this.httpClient = HttpClient.newHttpClient();
        
        loadApiConfig();
        initUI();
    }
    
    /**
     * 加载API配置
     */
    private void loadApiConfig() {
        try {
            Properties props = new Properties();
            props.load(new FileInputStream(CONFIG_FILE));
            apiKey = props.getProperty("api.key");
            if (apiKey == null || apiKey.isEmpty()) {
                displayMessage("系统", "警告：API密钥未配置，AI助手功能将无法正常工作。");
            }
        } catch (IOException e) {
            displayMessage("系统", "错误：无法加载API配置文件 - " + e.getMessage());
        }
    }
    
    /**
     * 调用API处理用户消息（非流式）
     */
    private String callApi(String prompt) throws IOException, InterruptedException {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IOException("API密钥未配置");
        }
        
        // 构建JSON请求体
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("model", "Pro/deepseek-ai/DeepSeek-V3");
        
        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);
        
        requestMap.put("messages", new Object[]{message});
        requestMap.put("temperature", 0.7);
        requestMap.put("max_tokens", 1000);
        requestMap.put("stream", false);
        
        // 将Map转换为JSON字符串
        String requestBody = mapToJsonString(requestMap);
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_ENDPOINT))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new IOException("API请求失败：" + response.body());
        }
        
        // 解析API响应，提取回答内容
        return parseApiResponse(response.body());
    }
    
    /**
     * 调用API处理用户消息（流式）
     * 
     * @param prompt 用户提示
     * @param callback 接收流式响应的回调函数
     */
    private void callApiStream(String prompt, Consumer<String> callback) throws IOException, InterruptedException {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IOException("API密钥未配置");
        }
        
        // 构建JSON请求体
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("model", "Pro/deepseek-ai/DeepSeek-V3");
        
        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);
        
        requestMap.put("messages", new Object[]{message});
        requestMap.put("temperature", 0.7);
        requestMap.put("max_tokens", 1000);
        requestMap.put("stream", true); // 启用流式输出
        
        // 将Map转换为JSON字符串
        String requestBody = mapToJsonString(requestMap);
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_ENDPOINT))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();
        
        // 使用InputStream处理流式响应
        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        
        if (response.statusCode() != 200) {
            throw new IOException("API请求失败，状态码：" + response.statusCode());
        }
        
        // 使用BufferedReader读取流式响应
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data: ")) {
                    String data = line.substring(6); // 去掉 "data: " 前缀
                    if (data.equals("[DONE]")) {
                        break; // 流式响应结束
                    }
                    
                    try {
                        // 解析JSON数据
                        int contentStart = data.indexOf("\"content\":\"");
                        if (contentStart != -1) {
                            contentStart += 11; // "content":"的长度
                            int contentEnd = data.indexOf("\"", contentStart);
                            if (contentEnd != -1) {
                                String content = data.substring(contentStart, contentEnd);
                                content = unescapeJsonString(content);
                                if (!content.isEmpty()) {
                                    callback.accept(content);
                                }
                            }
                        }
                    } catch (Exception e) {
                        // 忽略解析错误，继续处理下一行
                    }
                }
            }
        }
    }
    
   /**
    * 将Map转换为JSON字符串
    */
    private String mapToJsonString(Map<String, Object> map) {
        StringBuilder json = new StringBuilder("{\n");
        boolean first = true;

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                json.append(",\n");
            }
            first = false;

            json.append("  \"").append(entry.getKey()).append("\": ");
            Object value = entry.getValue();

            if (value instanceof String) {
                json.append("\"").append(escapeJsonString((String) value)).append("\"");
            } else if (value instanceof Number) {
                json.append(value);
            } else if (value instanceof Boolean) {
                json.append(value);
            } else if (value instanceof Object[]) {
                json.append("[");
                Object[] array = (Object[]) value;
                for (int i = 0; i < array.length; i++) {
                    if (i > 0) {
                        json.append(", ");
                    }
                    // 类型检查以确保安全转换
                    if (array[i] instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> subMap = (Map<String, Object>) array[i];
                        json.append(mapToJsonString(subMap));
                    } else if (array[i] instanceof String) {
                        json.append("\"").append(escapeJsonString((String) array[i])).append("\"");
                    } else {
                        json.append(array[i]); // 其他类型直接追加
                    }
                }
                json.append("]");
            }
        }

        json.append("\n}");
        return json.toString();
    }
    
    /**
     * 转义JSON字符串中的特殊字符
     */
    private String escapeJsonString(String input) {
        return input.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
    
    /**
     * 解析API响应
     */
    private String parseApiResponse(String responseBody) throws IOException {
        try {
            // 查找choices数组中第一个消息的content
            int choicesStart = responseBody.indexOf("\"choices\":");
            if (choicesStart == -1) {
                throw new IOException("无法在响应中找到choices字段");
            }
            
            int messageStart = responseBody.indexOf("\"message\":", choicesStart);
            if (messageStart == -1) {
                throw new IOException("无法在响应中找到message字段");
            }
            
            int contentStart = responseBody.indexOf("\"content\":", messageStart);
            if (contentStart == -1) {
                throw new IOException("无法在响应中找到content字段");
            }
            
            contentStart += 11; // "content": 的长度加上引号
            
            // 找到content值的结束位置
            int contentEnd = -1;
            boolean inEscape = false;
            int quoteCount = 0;
            
            for (int i = contentStart; i < responseBody.length(); i++) {
                char c = responseBody.charAt(i);
                
                if (inEscape) {
                    inEscape = false;
                    continue;
                }
                
                if (c == '\\') {
                    inEscape = true;
                    continue;
                }
                
                if (c == '"') {
                    quoteCount++;
                    if (quoteCount == 2) { // 找到闭合的引号
                        contentEnd = i;
                        break;
                    }
                }
            }
            
            if (contentEnd == -1) {
                throw new IOException("无法解析响应中的content值");
            }
            
            // 提取并解码content值
            String content = responseBody.substring(contentStart, contentEnd);
            return unescapeJsonString(content);
            
        } catch (Exception e) {
            throw new IOException("解析API响应失败：" + e.getMessage(), e);
        }
    }
    
    /**
     * 解码JSON字符串中的转义字符
     */
    private String unescapeJsonString(String input) {
        return input.replace("\\n", "\n")
                    .replace("\\r", "\r")
                    .replace("\\t", "\t")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
    }
    
    /**
     * 初始化UI组件
     */
    private void initUI() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // 创建聊天显示区域
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        
        JScrollPane scrollPane = new JScrollPane(chatArea);
        add(scrollPane, BorderLayout.CENTER);
        
        // 创建快捷功能按钮面板
        JPanel quickActionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        quickActionPanel.setBorder(BorderFactory.createTitledBorder("快捷分析功能"));
        
        // 添加快捷功能按钮
        JButton spendingAnalysisButton = new JButton("支出分析");
        spendingAnalysisButton.addActionListener(e -> triggerAnalysis("请分析我的支出情况，找出主要支出类别和趋势。"));
        quickActionPanel.add(spendingAnalysisButton);
        
        JButton budgetSuggestionButton = new JButton("预算建议");
        budgetSuggestionButton.addActionListener(e -> triggerAnalysis("根据我的消费习惯，请给我提供合理的预算建议。"));
        quickActionPanel.add(budgetSuggestionButton);
        
        JButton savingOpportunitiesButton = new JButton("节省机会");
        savingOpportunitiesButton.addActionListener(e -> triggerAnalysis("请分析我的支出，找出可能的节省机会。"));
        quickActionPanel.add(savingOpportunitiesButton);
        
        JButton seasonalPatternsButton = new JButton("季节性模式");
        seasonalPatternsButton.addActionListener(e -> triggerAnalysis("请分析我的消费是否存在季节性模式。"));
        quickActionPanel.add(seasonalPatternsButton);
        
        // 创建输入面板
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
        
        // 创建输入框
        inputField = new JTextField();
        inputField.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        inputField.addActionListener(e -> sendMessage()); // 添加回车发送功能
        inputPanel.add(inputField, BorderLayout.CENTER);
        
        // 创建发送按钮
        sendButton = new JButton("发送");
        sendButton.addActionListener(e -> sendMessage());
        inputPanel.add(sendButton, BorderLayout.EAST);
        
        // 组合底部面板
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(quickActionPanel, BorderLayout.NORTH);
        bottomPanel.add(inputPanel, BorderLayout.SOUTH);
        
        // 添加底部面板
        add(bottomPanel, BorderLayout.SOUTH);
        
        // 显示欢迎消息
        displayMessage("AI助手", "您好！我是您的AI财务助手。我可以帮您分析支出情况，提供预算建议，以及回答您关于财务管理的问题。\n\n您可以直接与我聊天，或使用上方的快捷分析功能按钮。");
    }
    
    /**
     * 触发特定分析功能
     */
    private void triggerAnalysis(String analysisPrompt) {
        // 显示用户请求
        displayMessage("您", analysisPrompt);
        
        // 处理分析请求
        processAnalysisRequest(analysisPrompt);
    }
    
    /**
     * 发送消息
     */
    private void sendMessage() {
        String message = inputField.getText().trim();
        if (!message.isEmpty()) {
            // 显示用户消息
            displayMessage("您", message);
            
            // 清空输入框
            inputField.setText("");
            
            // 处理用户消息
            processUserMessage(message);
        }
    }
    
    /**
     * 判断是否是简单问候
     */
    private boolean isSimpleGreeting(String message) {
        message = message.toLowerCase().trim();
        String[] greetings = {"你好", "早上好", "下午好", "晚上好", "嗨", "hi", "hello", "hey", "哈喽", "您好"};
        
        for (String greeting : greetings) {
            if (message.contains(greeting)) {
                return true;
            }
        }
        
        // 检查是否是简短的问候语（少于10个字符）
        if (message.length() < 10) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 处理用户消息
     */
    private void processUserMessage(String message) {
        // 判断是否是简单问候
        if (isSimpleGreeting(message)) {
            // 如果是简单问候，直接回复，不进行数据分析
            String greeting = "您好！很高兴为您服务。您可以直接询问我关于您的财务情况，或使用上方的快捷分析功能按钮。";
            displayMessage("AI助手", greeting);
            return;
        }
        
        // 如果不是简单问候，进行数据分析
        processAnalysisRequest(message);
    }
    
    /**
     * 处理分析请求
     */
    private void processAnalysisRequest(String message) {
        try {
            // 获取所有交易记录
            List<Transaction> transactions = transactionManager.getAllTransactions();
            
            if (transactions.isEmpty()) {
                displayMessage("AI助手", "目前还没有任何交易记录。请先添加一些交易记录，我才能为您提供分析和建议。");
                return;
            }
            
            // 显示处理中消息
            displayMessage("系统", "正在分析您的问题，请稍候...");
            
            // 禁用发送按钮，防止重复发送
            sendButton.setEnabled(false);
            inputField.setEnabled(false);
            
            // 将交易记录转换为CSV格式，使用StringBuilder提高性能
            StringBuilder csvData = new StringBuilder(1024);
            csvData.append("ID,金额,日期,类别,描述,类型,支付方式\n");
            
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            for (Transaction t : transactions) {
                csvData.append(t.getId()).append(',');
                csvData.append(String.format("%.2f", t.getAmount())).append(',');
                csvData.append(t.getDate().format(dateFormatter)).append(',');
                csvData.append(escapeCSV(t.getCategory())).append(',');
                csvData.append(escapeCSV(t.getDescription())).append(',');
                csvData.append(t.isExpense() ? "支出" : "收入").append(',');
                csvData.append(escapeCSV(t.getPaymentMethod())).append('\n');
            }
            
            // 使用分析器提供的统计数据增强提示
            String statistics = analyzer.getBasicStatistics(transactions);
            
            // 构建更详细的分析提示
            final String analysisPrompt = String.format(
                "你是一个专业的财务分析助手。请基于以下交易数据和统计信息，以专业且友好的口吻回答用户问题：\n\n" +
                "交易数据：\n%s\n\n" +
                "统计信息：\n%s\n\n" +
                "用户问题：%s\n\n" +
                "请提供具体的分析和建议，包括支出趋势、预算建议和财务优化方案。",
                csvData.toString(),
                statistics,
                message
            );
            
            // 创建AI助手回复的占位符
            currentAIResponseId = "ai_" + System.currentTimeMillis();
            displayMessage("AI助手", ""); // 创建空消息，稍后会通过流式输出填充内容
            
            // 使用SwingWorker在后台线程中调用流式API
            new SwingWorker<Void, String>() {
                @Override
                protected Void doInBackground() throws Exception {
                    try {
                        // 在后台线程中调用流式API
                        callApiStream(analysisPrompt, chunk -> {
                            // 发布进度，将接收到的文本块传递给process方法
                            publish(chunk);
                        });
                    } catch (Exception e) {
                        throw e;
                    }
                    return null;
                }
                
                @Override
                protected void process(List<String> chunks) {
                    // 在EDT线程中处理接收到的文本块
                    for (String chunk : chunks) {
                        appendToMessage("AI助手", chunk);
                    }
                }
                
                @Override
                protected void done() {
                    try {
                        get(); // 检查是否有异常
                        currentAIResponseId = null; // 重置当前响应ID
                    } catch (InterruptedException | ExecutionException e) {
                        Throwable cause = e.getCause();
                        if (cause instanceof IOException && cause.getMessage().contains("API密钥未配置")) {
                            displayMessage("AI助手", "抱歉，AI助手功能暂时无法使用，因为API密钥未配置。请联系管理员配置API密钥。");
                        } else {
                            displayMessage("AI助手", "抱歉，调用AI服务时出现错误：" + (cause != null ? cause.getMessage() : e.getMessage()));
                        }
                    } finally {
                        // 重新启用发送按钮和输入框
                        sendButton.setEnabled(true);
                        inputField.setEnabled(true);
                    }
                }
            }.execute();
            
        } catch (Exception e) {
            displayMessage("AI助手", "抱歉，处理交易数据时出现错误：" + e.getMessage());
            // 确保发送按钮和输入框被重新启用
            sendButton.setEnabled(true);
            inputField.setEnabled(true);
        }
    }
    
    /**
     * 转义CSV字段中的特殊字符
     */
    private String escapeCSV(String field) {
        if (field == null) return "";
        // 如果字段包含逗号、换行符或双引号，需要用双引号包围并对内部的双引号进行转义
        if (field.contains(",") || field.contains("\n") || field.contains("\"")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }
    
    /**
     * 显示消息
     */
    private void displayMessage(String sender, String message) {
        String formattedMessage = String.format("[%s]: %s\n\n", sender, message);
        
        // 如果是AI助手的消息，保存当前位置以便后续追加
        if (sender.equals("AI助手")) {
            currentAIResponseId = "ai_" + System.currentTimeMillis();
        } else {
            currentAIResponseId = null;
        }
        
        chatArea.append(formattedMessage);
        // 滚动到最新消息
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }
    
    /**
     * 追加内容到最后一条AI助手消息
     */
    private void appendToMessage(String sender, String chunk) {
        if (!sender.equals("AI助手") || currentAIResponseId == null) {
            displayMessage(sender, chunk);
            return;
        }
        
        // 追加内容到聊天区域
        chatArea.append(chunk);
        
        // 滚动到最新消息
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }
}
