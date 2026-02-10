package com.bobo.llm4j.platform.qwen.chat;

import com.bobo.llm4j.chat.entity.ChatResponse;
import com.bobo.llm4j.chat.entity.Prompt;
import com.bobo.llm4j.chat.entity.StreamOptions;
import com.bobo.llm4j.chat.model.ChatModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.bobo.llm4j.config.QwenConfig;
import com.bobo.llm4j.constant.Constants;
import com.bobo.llm4j.http.StreamingResponseHandler;
import com.bobo.llm4j.config.Configuration;
import com.bobo.llm4j.http.Flux;
import com.bobo.llm4j.utils.ValidateUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.sse.EventSource;

/**
 * QwenChatModel - 千问(Qwen) Chat模型实现 (OpenAI兼容模式)
 * <p>
 * 千问提供了OpenAI兼容的API接口，因此实现方式与OpenAI类似：
 * <ul>
 *   <li>配置 (apiKey, baseUrl) 通过构造函数注入</li>
 *   <li>实现统一的 ChatModel 接口</li>
 *   <li>支持同步调用 call() 和流式调用 stream()</li>
 *   <li>Prompt 中的运行时选项可以覆盖默认选项</li>
 * </ul>
 * </p>
 * 
 * @see com.bobo.llm4j.chat.model.ChatModel
 * @see QwenConfig
 * @author bobo
 * @since 1.0.0
 */
@Slf4j
public class QwenChatModel implements ChatModel {

    private final QwenConfig qwenConfig;
    private final OkHttpClient okHttpClient;
    private final EventSource.Factory factory;

    /**
     * 构造函数 - 使用 Configuration 中的 QwenConfig
     * 
     * @param configuration 统一配置对象
     */
    public QwenChatModel(Configuration configuration) {
        this.qwenConfig = configuration.getQwenConfig();
        this.okHttpClient = configuration.getOkHttpClient();
        this.factory = configuration.createRequestFactory();
    }

    /**
     * 构造函数 - 直接指定 QwenConfig
     * 
     * @param configuration 统一配置对象（用于获取 OkHttpClient）
     * @param qwenConfig 千问配置
     */
    public QwenChatModel(Configuration configuration, QwenConfig qwenConfig) {
        this.qwenConfig = qwenConfig;
        this.okHttpClient = configuration.getOkHttpClient();
        this.factory = configuration.createRequestFactory();
    }

    @Override
    public ChatResponse call(Prompt prompt) throws Exception {
        return internalCall(null, null, prompt);
    }

    /**
     * 内部调用方法，支持可选的 baseUrl 和 apiKey 覆盖
     * 
     * @param baseUrl 可选的基础URL覆盖（如果为null则使用配置中的值）
     * @param apiKey 可选的API Key覆盖（如果为null则使用配置中的值）
     * @param prompt 提示词
     * @return 聊天响应
     * @throws Exception 如果发生错误
     */
    private ChatResponse internalCall(String baseUrl, String apiKey, Prompt prompt) throws Exception {
        if(baseUrl == null || baseUrl.isEmpty()) baseUrl = qwenConfig.getApiHost();
        if(apiKey == null || apiKey.isEmpty()) apiKey = qwenConfig.getApiKey();
        
        // 设置为非流式模式
        prompt.setStream(false);
        prompt.setStreamOptions(null);

        ObjectMapper mapper = new ObjectMapper();
        String requestString = mapper.writeValueAsString(prompt);
        
        log.debug("Qwen API Request: {}", requestString);

        // 千问使用 Authorization: Bearer {apiKey} 格式（OpenAI兼容）
        Request request = new Request.Builder()
                .header("Authorization", "Bearer " + apiKey)
                .url(ValidateUtil.concatUrl(baseUrl, qwenConfig.getChatCompletionUrl()))
                .post(RequestBody.create(requestString, MediaType.parse(Constants.JSON_CONTENT_TYPE)))
                .build();

        Response execute = okHttpClient.newCall(request).execute();
        if (execute.isSuccessful() && execute.body() != null){
            String responseBody = execute.body().string();
            log.debug("Qwen API Response: {}", responseBody);
            return mapper.readValue(responseBody, ChatResponse.class);
        } else {
            String errorBody = execute.body() != null ? execute.body().string() : "No error body";
            log.error("Qwen API Error: Status={}, Body={}", execute.code(), errorBody);
            throw new RuntimeException("Qwen API call failed: " + execute.code() + " - " + errorBody);
        }
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) throws Exception {
        return internalStream(null, null, prompt);
    }

    /**
     * 内部流式调用方法，支持可选的 baseUrl 和 apiKey 覆盖
     * <p>
     * 此方法创建一个响应式 Flux，当从千问流式API（Server-Sent Events）
     * 接收到响应时，会发出 ChatResponse 对象。
     * </p>
     * 
     * @param baseUrl 可选的基础URL覆盖（如果为null则使用配置中的值）
     * @param apiKey 可选的API Key覆盖（如果为null则使用配置中的值）
     * @param prompt 提示词
     * @return ChatResponse 的 Flux 流
     * @throws Exception 如果发生错误
     */
    private Flux<ChatResponse> internalStream(String baseUrl, String apiKey, Prompt prompt) throws Exception {
        final String finalBaseUrl = (baseUrl == null || baseUrl.isEmpty()) ? qwenConfig.getApiHost() : baseUrl;
        final String finalApiKey = (apiKey == null || apiKey.isEmpty()) ? qwenConfig.getApiKey() : apiKey;
        
        // 设置为流式模式
        prompt.setStream(true);
        StreamOptions streamOptions = prompt.getStreamOptions();
        if(streamOptions == null){
            prompt.setStreamOptions(new StreamOptions(true));
        }

        // 创建 Flux，用于发出 ChatResponse 对象
        return Flux.create(sink -> {
            try {
                // 创建流式处理器，将数据发送到 Flux
                StreamingResponseHandler handler = new StreamingResponseHandler() {
                    @Override
                    protected void onChunk(ChatResponse response) {
                        try {
                            // 将响应发送到 Flux
                            sink.next(response);
                        } catch (Exception e) {
                            log.error("Error emitting chunk to Flux", e);
                            sink.error(e);
                        }
                    }
                    
                    @Override
                    protected void onComplete() {
                        log.debug("Qwen streaming completed");
                        sink.complete();
                    }
                    
                    @Override
                    protected void onError(Throwable t, Response response) {
                        log.error("Qwen streaming error", t);
                        if (response != null && !response.isSuccessful()) {
                            try {
                                String errorBody = response.body() != null ? 
                                    response.body().string() : "";
                                sink.error(new RuntimeException(
                                    "Qwen API error (status " + response.code() + "): " + errorBody, t));
                            } catch (Exception e) {
                                sink.error(t != null ? t : e);
                            }
                        } else {
                            sink.error(t != null ? t : 
                                new RuntimeException("Unknown streaming error"));
                        }
                    }
                };

                // 构建请求
                ObjectMapper mapper = new ObjectMapper();
                String jsonString = mapper.writeValueAsString(prompt);
                
                log.debug("Qwen Streaming Request: {}", jsonString);

                Request request = new Request.Builder()
                        .header("Authorization", "Bearer " + finalApiKey)
                        .url(ValidateUtil.concatUrl(finalBaseUrl, qwenConfig.getChatCompletionUrl()))
                        .post(RequestBody.create(jsonString, MediaType.parse(Constants.APPLICATION_JSON)))
                        .build();

                // 启动 SSE 连接
                factory.newEventSource(request, handler);
                
            } catch (Exception e) {
                log.error("Error setting up Qwen streaming", e);
                sink.error(e);
            }
        });
    }

    /**
     * 旧版流式调用方法（使用回调处理器）
     * @deprecated 使用 {@link #stream(Prompt)} 代替，它返回 Flux
     */
    @Deprecated
    public void streamWithHandler(Prompt prompt, StreamingResponseHandler handler) throws Exception {
        streamWithHandler(null, null, prompt, handler);
    }

    /**
     * 旧版流式调用方法（使用回调处理器，支持URL/key覆盖）
     * @deprecated 使用 {@link #stream(Prompt)} 代替，它返回 Flux
     */
    @Deprecated
    public void streamWithHandler(String baseUrl, String apiKey, Prompt prompt, StreamingResponseHandler handler) throws Exception {
        if(baseUrl == null || baseUrl.isEmpty()) baseUrl = qwenConfig.getApiHost();
        if(apiKey == null || apiKey.isEmpty()) apiKey = qwenConfig.getApiKey();
        prompt.setStream(true);
        StreamOptions streamOptions = prompt.getStreamOptions();
        if(streamOptions == null){
            prompt.setStreamOptions(new StreamOptions(true));
        }

        ObjectMapper mapper = new ObjectMapper();
        String jsonString = mapper.writeValueAsString(prompt);

        Request request = new Request.Builder()
                .header("Authorization", "Bearer " + apiKey)
                .url(ValidateUtil.concatUrl(baseUrl, qwenConfig.getChatCompletionUrl()))
                .post(RequestBody.create(jsonString, MediaType.parse(Constants.APPLICATION_JSON)))
                .build();

        factory.newEventSource(request, handler);
        handler.getCountDownLatch().await();
    }
}
