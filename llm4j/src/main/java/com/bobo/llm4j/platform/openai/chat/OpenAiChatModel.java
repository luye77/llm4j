package com.bobo.llm4j.platform.openai.chat;

import com.bobo.llm4j.chat.entity.ChatResponse;
import com.bobo.llm4j.chat.entity.ChatTool;
import com.bobo.llm4j.chat.entity.Prompt;
import com.bobo.llm4j.chat.entity.StreamOptions;
import com.bobo.llm4j.chat.model.ChatModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.bobo.llm4j.config.OpenAiConfig;
import com.bobo.llm4j.constant.Constants;
import com.bobo.llm4j.http.StreamingResponseHandler;
import com.bobo.llm4j.config.Configuration;
import com.bobo.llm4j.http.Flux;
import com.bobo.llm4j.tool.DefaultToolCallingManager;
import com.bobo.llm4j.tool.DefaultToolExecutionEligibilityPredicate;
import com.bobo.llm4j.tool.ToolCallingManager;
import com.bobo.llm4j.tool.ToolDefinition;
import com.bobo.llm4j.tool.ToolExecutionEligibilityPredicate;
import com.bobo.llm4j.tool.ToolExecutionResult;
import com.bobo.llm4j.utils.ValidateUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.sse.EventSource;

import java.util.ArrayList;
import java.util.List;

/**
 * OpenAiChatModel - OpenAI Chat模型实现 (Spring AI compatible)
 * <p>
 * This implementation follows Spring AI's design principles:
 * <ul>
 *   <li>Configuration (apiKey, baseUrl) is injected via constructor</li>
 *   <li>Implements the unified ChatModel interface</li>
 *   <li>Supports both synchronous call() and streaming via Flux</li>
 *   <li>Runtime options in Prompt can override default options</li>
 * </ul>
 * </p>
 * 
 * @see com.bobo.llm4j.chat.model.ChatModel
 * @see OpenAiConfig
 */
@Slf4j
public class OpenAiChatModel implements ChatModel {

    private final OpenAiConfig openAiConfig;
    private final OkHttpClient okHttpClient;
    private final EventSource.Factory factory;
    private final ToolCallingManager toolCallingManager;
    private final ToolExecutionEligibilityPredicate toolExecutionEligibilityPredicate;

    public OpenAiChatModel(Configuration configuration) {
        this.openAiConfig = configuration.getOpenAiConfig();
        this.okHttpClient = configuration.getOkHttpClient();
        this.factory = configuration.createRequestFactory();
        this.toolCallingManager = new DefaultToolCallingManager();
        this.toolExecutionEligibilityPredicate = new DefaultToolExecutionEligibilityPredicate();
    }

    public OpenAiChatModel(Configuration configuration, OpenAiConfig openAiConfig) {
        this.openAiConfig = openAiConfig;
        this.okHttpClient = configuration.getOkHttpClient();
        this.factory = configuration.createRequestFactory();
        this.toolCallingManager = new DefaultToolCallingManager();
        this.toolExecutionEligibilityPredicate = new DefaultToolExecutionEligibilityPredicate();
    }

    @Override
    public ChatResponse call(Prompt prompt) throws Exception {
        Prompt requestPrompt = enrichPromptWithToolDefinitions(prompt);
        ChatResponse response = internalCall(null, null, requestPrompt);
        int depth = 0;
        while (toolExecutionEligibilityPredicate.isToolExecutionRequired(requestPrompt, response)) {
            ToolExecutionResult executionResult = toolCallingManager.executeToolCalls(requestPrompt, response);
            requestPrompt = requestPrompt.toBuilder()
                    .messages(executionResult.getConversationHistory())
                    .build();
            requestPrompt = enrichPromptWithToolDefinitions(requestPrompt);
            response = internalCall(null, null, requestPrompt);
            depth++;
            if (depth > 8) {
                throw new IllegalStateException("Tool calling exceeded max depth (8)");
            }
        }
        return response;
    }

    /**
     * Internal call method with optional baseUrl and apiKey override
     * 
     * @param baseUrl optional base URL override (uses config if null)
     * @param apiKey optional API key override (uses config if null)
     * @param prompt the prompt
     * @return chat response
     * @throws Exception if error occurs
     */
    private ChatResponse internalCall(String baseUrl, String apiKey, Prompt prompt) throws Exception {
        if(baseUrl == null || baseUrl.isEmpty()) baseUrl = openAiConfig.getApiHost();
        if(apiKey == null || apiKey.isEmpty()) apiKey = openAiConfig.getApiKey();
        prompt.setStream(false);
        prompt.setStreamOptions(null);

        ObjectMapper mapper = new ObjectMapper();
        String requestString = mapper.writeValueAsString(prompt);

        Request request = new Request.Builder()
                .header("Authorization", "Bearer " + apiKey)
                .url(ValidateUtil.concatUrl(baseUrl, openAiConfig.getChatCompletionUrl()))
                .post(RequestBody.create(requestString, MediaType.parse(Constants.JSON_CONTENT_TYPE)))
                .build();

        Response execute = okHttpClient.newCall(request).execute();
        if (execute.isSuccessful() && execute.body() != null){
            return mapper.readValue(execute.body().string(), ChatResponse.class);
        }else{
            return null;
        }
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) throws Exception {
        return internalStream(null, null, prompt);
    }

    /**
     * Internal stream method with optional baseUrl and apiKey override
     * <p>
     * This method creates a reactive Flux that emits ChatResponse objects as they
     * are received from the OpenAI streaming API (Server-Sent Events).
     * </p>
     * 
     * @param baseUrl optional base URL override (uses config if null)
     * @param apiKey optional API key override (uses config if null)
     * @param prompt the prompt
     * @return Flux of chat responses
     * @throws Exception if error occurs
     */
    private Flux<ChatResponse> internalStream(String baseUrl, String apiKey, Prompt prompt) throws Exception {
        final String finalBaseUrl = (baseUrl == null || baseUrl.isEmpty()) ? openAiConfig.getApiHost() : baseUrl;
        final String finalApiKey = (apiKey == null || apiKey.isEmpty()) ? openAiConfig.getApiKey() : apiKey;
        
        // Set streaming mode
        prompt.setStream(true);
        StreamOptions streamOptions = prompt.getStreamOptions();
        if(streamOptions == null){
            prompt.setStreamOptions(new StreamOptions(true));
        }

        // Create Flux that will emit ChatResponse objects
        return Flux.create(sink -> {
            try {
                // Create streaming handler that emits to the Flux
                StreamingResponseHandler handler = new StreamingResponseHandler() {
                    @Override
                    protected void onChunk(ChatResponse response) {
                        try {
                            // Emit the response to the Flux
                            sink.next(response);
                        } catch (Exception e) {
                            log.error("Error emitting chunk to Flux", e);
                            sink.error(e);
                        }
                    }
                    
                    @Override
                    protected void onComplete() {
                        log.debug("Streaming completed");
                        sink.complete();
                    }
                    
                    @Override
                    protected void onError(Throwable t, Response response) {
                        log.error("Streaming error", t);
                        if (response != null && !response.isSuccessful()) {
                            try {
                                String errorBody = response.body() != null ? 
                                    response.body().string() : "";
                                sink.error(new RuntimeException(
                                    "OpenAI API error (status " + response.code() + "): " + errorBody, t));
                            } catch (Exception e) {
                                sink.error(t != null ? t : e);
                            }
                        } else {
                            sink.error(t != null ? t : 
                                new RuntimeException("Unknown streaming error"));
                        }
                    }
                };

                // Build request
                ObjectMapper mapper = new ObjectMapper();
                String jsonString = mapper.writeValueAsString(prompt);

                Request request = new Request.Builder()
                        .header("Authorization", "Bearer " + finalApiKey)
                        .url(ValidateUtil.concatUrl(finalBaseUrl, openAiConfig.getChatCompletionUrl()))
                        .post(RequestBody.create(jsonString, MediaType.parse(Constants.APPLICATION_JSON)))
                        .build();

                // Start SSE connection
                factory.newEventSource(request, handler);
                
            } catch (Exception e) {
                log.error("Error setting up streaming", e);
                sink.error(e);
            }
        });
    }

    private Prompt enrichPromptWithToolDefinitions(Prompt prompt) {
        if (prompt == null) {
            return null;
        }
        if (prompt.getTools() != null && !prompt.getTools().isEmpty()) {
            return prompt;
        }
        List<ToolDefinition> toolDefinitions = toolCallingManager.resolveToolDefinitions(prompt);
        if (toolDefinitions.isEmpty()) {
            return prompt;
        }
        List<ChatTool> requestTools = new ArrayList<ChatTool>();
        for (ToolDefinition definition : toolDefinitions) {
            requestTools.add(ChatTool.fromDefinition(definition));
        }
        return prompt.toBuilder().tools(requestTools).build();
    }

    /**
     * Legacy stream method with callback handler
     * @deprecated Use {@link #stream(Prompt)} instead which returns Flux
     */
    @Deprecated
    public void streamWithHandler(Prompt prompt, StreamingResponseHandler handler) throws Exception {
        streamWithHandler(null, null, prompt, handler);
    }

    /**
     * Legacy stream method with callback handler and URL/key override
     * @deprecated Use {@link #stream(Prompt)} instead which returns Flux
     */
    @Deprecated
    public void streamWithHandler(String baseUrl, String apiKey, Prompt prompt, StreamingResponseHandler handler) throws Exception {
        if(baseUrl == null || baseUrl.isEmpty()) baseUrl = openAiConfig.getApiHost();
        if(apiKey == null || apiKey.isEmpty()) apiKey = openAiConfig.getApiKey();
        prompt.setStream(true);
        StreamOptions streamOptions = prompt.getStreamOptions();
        if(streamOptions == null){
            prompt.setStreamOptions(new StreamOptions(true));
        }

        ObjectMapper mapper = new ObjectMapper();
        String jsonString = mapper.writeValueAsString(prompt);

        Request request = new Request.Builder()
                .header("Authorization", "Bearer " + apiKey)
                .url(ValidateUtil.concatUrl(baseUrl, openAiConfig.getChatCompletionUrl()))
                .post(RequestBody.create(jsonString, MediaType.parse(Constants.APPLICATION_JSON)))
                .build();

        factory.newEventSource(request, handler);
        handler.getCountDownLatch().await();
    }
}
