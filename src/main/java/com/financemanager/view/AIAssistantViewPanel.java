package com.financemanager.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

public class AIAssistantViewPanel extends JPanel {

    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton;
    private Consumer<String> sendMessageConsumer; // 用于处理发送消息的逻辑
    private Consumer<String> triggerAnalysisConsumer; // 用于处理快捷分析的逻辑

    public AIAssistantViewPanel() {
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));

        JScrollPane scrollPane = new JScrollPane(chatArea);
        add(scrollPane, BorderLayout.CENTER);

        JPanel quickActionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        quickActionPanel.setBorder(BorderFactory.createTitledBorder("快捷分析功能"));

        JButton spendingAnalysisButton = new JButton("支出分析");
        spendingAnalysisButton.addActionListener(e -> {
            if (triggerAnalysisConsumer != null) {
                triggerAnalysisConsumer.accept("请分析我的支出情况，找出主要支出类别和趋势。");
            }
        });
        quickActionPanel.add(spendingAnalysisButton);

        JButton budgetSuggestionButton = new JButton("预算建议");
        budgetSuggestionButton.addActionListener(e -> {
            if (triggerAnalysisConsumer != null) {
                triggerAnalysisConsumer.accept("根据我的消费习惯，请给我提供合理的预算建议。");
            }
        });
        quickActionPanel.add(budgetSuggestionButton);

        JButton savingOpportunitiesButton = new JButton("节省机会");
        savingOpportunitiesButton.addActionListener(e -> {
            if (triggerAnalysisConsumer != null) {
                triggerAnalysisConsumer.accept("请分析我的支出，找出可能的节省机会。");
            }
        });
        quickActionPanel.add(savingOpportunitiesButton);

        JButton seasonalPatternsButton = new JButton("季节性模式");
        seasonalPatternsButton.addActionListener(e -> {
            if (triggerAnalysisConsumer != null) {
                triggerAnalysisConsumer.accept("请分析我的消费是否存在季节性模式。");
            }
        });
        quickActionPanel.add(seasonalPatternsButton);

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

        inputField = new JTextField();
        inputField.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        inputField.addActionListener(e -> sendMessage());
        inputPanel.add(inputField, BorderLayout.CENTER);

        sendButton = new JButton("发送");
        sendButton.addActionListener(e -> sendMessage());
        inputPanel.add(sendButton, BorderLayout.EAST);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(quickActionPanel, BorderLayout.NORTH);
        bottomPanel.add(inputPanel, BorderLayout.SOUTH);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void sendMessage() {
        String message = inputField.getText().trim();
        if (!message.isEmpty() && sendMessageConsumer != null) {
            sendMessageConsumer.accept(message); // 将消息传递给控制器处理
            inputField.setText(""); // 清空输入框由控制器处理后决定，或在此处清空
        }
    }
    
    public void setSendMessageConsumer(Consumer<String> consumer) {
        this.sendMessageConsumer = consumer;
    }

    public void setTriggerAnalysisConsumer(Consumer<String> consumer) {
        this.triggerAnalysisConsumer = consumer;
    }

    public void displayMessage(String sender, String message) {
        String formattedMessage = String.format("[%s]: %s\n\n", sender, message);
        chatArea.append(formattedMessage);
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    public void appendToMessage(String chunk) {
        // 移除Markdown星号，可以根据需要扩展到其他Markdown字符
        String cleanedChunk = chunk.replace("*", ""); 
        chatArea.append(cleanedChunk);
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    public void setInputEnabled(boolean enabled) {
        inputField.setEnabled(enabled);
        sendButton.setEnabled(enabled);
    }
    
    public String getInputText() {
        return inputField.getText();
    }

    public void clearInputField() {
        inputField.setText("");
    }
}
