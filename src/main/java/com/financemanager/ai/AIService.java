package com.financemanager.ai;

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
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;

public class AIService { 

    private static final String CONFIG_FILE = "config.properties";
    private static final String API_ENDPOINT = "https://openrouter.ai/api/v1/chat/completions";
    private String apiKey;
    private final HttpClient httpClient;

    public AIService() { 
        this.httpClient = HttpClient.newHttpClient();
        loadApiConfig();
    }

    private void loadApiConfig() {
        try {
            Properties props = new Properties();
            // Try loading from classpath first
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE);
            if (inputStream == null) {
                // Fallback to file system if not found in classpath
                inputStream = new FileInputStream(CONFIG_FILE);
            }
            props.load(inputStream);
            apiKey = props.getProperty("api.key");
            if (apiKey == null || apiKey.isEmpty()) {
                System.err.println("警告：API密钥未配置，AI助手功能将无法正常工作。请在 " + CONFIG_FILE + " 中配置 api.key");
            }
        } catch (IOException e) {
            System.err.println("错误：无法加载API配置文件 " + CONFIG_FILE + " - " + e.getMessage());
        }
    }

    public void callApiStream(String prompt, Consumer<String> callback) throws IOException, InterruptedException {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IOException("API密钥未配置");
        }

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("model", "deepseek/deepseek-chat-v3-0324"); 

        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);

        requestMap.put("messages", new Object[]{message});
        requestMap.put("temperature", 0.7);
        requestMap.put("max_tokens", 1000);
        requestMap.put("stream", true);

        String requestBody = mapToJsonString(requestMap);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_ENDPOINT))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            throw new IOException("API请求失败，状态码：" + response.statusCode() + ", 响应体: " + new String(response.body().readAllBytes(), StandardCharsets.UTF_8));
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data: ")) {
                    String data = line.substring(6);
                    if (data.equals("[DONE]")) {
                        break;
                    }
                    try {
                        int contentStart = data.indexOf("\"content\":\"");
                        if (contentStart != -1) {
                            contentStart += 11;
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
                        System.err.println("解析流式响应数据时出错: " + data + " - " + e.getMessage());
                    }
                }
            }
        }
    }

    public String getLLMChatCompletion(String prompt) throws IOException, InterruptedException {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IOException("API密钥未配置");
        }

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("model", "deepseek/deepseek-chat-v3-0324");

        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);

        requestMap.put("messages", new Object[]{message});
        requestMap.put("temperature", 0.7); // You might want to adjust this for classification
        requestMap.put("max_tokens", 150); // Classification usually needs fewer tokens
        requestMap.put("stream", false); // Set stream to false for a single response

        String requestBody = mapToJsonString(requestMap);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_ENDPOINT))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() != 200) {
            throw new IOException("API请求失败，状态码：" + response.statusCode() + ", 响应体: " + response.body());
        }

        // Parse the JSON response to extract the content
        // This is a simplified parser. For robust parsing, consider a JSON library like Jackson or Gson.
        String responseBody = response.body();
        try {
            // Example of a simple manual parse, assuming a structure like:
            // {"choices":[{"message":{"content":"CategoryName"}}]}
            // This will need to be adjusted based on the actual API response structure for non-streaming.
            // For OpenRouter, the non-streaming response for chat completions is typically:
            // {
            //   "id": "...",
            //   "object": "chat.completion",
            //   "created": ...,
            //   "model": "...",
            //   "choices": [
            //     {
            //       "index": 0,
            //       "message": {
            //         "role": "assistant",
            //         "content": "The actual response content"
            //       },
            //       "finish_reason": "stop"
            //     }
            //   ],
            //   "usage": { ... }
            // }

            int choicesStart = responseBody.indexOf("\"choices\":[");
            if (choicesStart != -1) {
                int messageStart = responseBody.indexOf("\"message\":{", choicesStart);
                if (messageStart != -1) {
                    int contentStart = responseBody.indexOf("\"content\":\"", messageStart);
                    if (contentStart != -1) {
                        contentStart += "\"content\":\"".length();
                        int contentEnd = responseBody.indexOf("\"", contentStart);
                        if (contentEnd != -1) {
                            return unescapeJsonString(responseBody.substring(contentStart, contentEnd));
                        }
                    }
                }
            }
            // If parsing fails, return the raw body or throw an error
            // For simplicity here, we'll throw an error if content not found.
            throw new IOException("无法从API响应中解析内容: " + responseBody);
        } catch (Exception e) {
            System.err.println("解析API响应时出错: " + responseBody + " - " + e.getMessage());
            throw new IOException("解析API响应时出错: " + e.getMessage(), e);
        }
    }

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
            } else if (value instanceof Number || value instanceof Boolean) {
                json.append(value);
            } else if (value instanceof Object[]) {
                json.append("[");
                Object[] array = (Object[]) value;
                for (int i = 0; i < array.length; i++) {
                    if (i > 0) {
                        json.append(", ");
                    }
                    if (array[i] instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> subMap = (Map<String, Object>) array[i];
                        json.append(mapToJsonString(subMap));
                    } else if (array[i] instanceof String) {
                        json.append("\"").append(escapeJsonString((String) array[i])).append("\"");
                    } else {
                        json.append(array[i]);
                    }
                }
                json.append("]");
            }
        }
        json.append("\n}");
        return json.toString();
    }

    private String escapeJsonString(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }

    private String unescapeJsonString(String input) {
        if (input == null) return "";
        return input.replace("\\n", "\n")
                    .replace("\\r", "\r")
                    .replace("\\t", "\t")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
    }
}
