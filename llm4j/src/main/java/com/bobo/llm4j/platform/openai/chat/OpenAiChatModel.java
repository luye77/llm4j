package com.bobo.llm4j.platform.openai.chat;

import com.bobo.llm4j.chat.entity.ChatResponse;
import com.bobo.llm4j.chat.entity.Prompt;
import com.bobo.llm4j.chat.entity.StreamOptions;
import com.bobo.llm4j.chat.model.ChatModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.bobo.llm4j.config.OpenAiConfig;
import com.bobo.llm4j.constant.Constants;
import com.bobo.llm4j.http.StreamingResponseHandler;
import com.bobo.llm4j.config.Configuration;
import com.bobo.llm4j.http.Flux;
import com.bobo.llm4j.utils.ValidateUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.sse.EventSource;

import java.util.concurrent.atomic.AtomicReference;

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

    public OpenAiChatModel(Configuration configuration) {
        this.openAiConfig = configuration.getOpenAiConfig();
        this.okHttpClient = configuration.getOkHttpClient();
        this.factory = configuration.createRequestFactory();
    }

    public OpenAiChatModel(Configuration configuration, OpenAiConfig openAiConfig) {
        this.openAiConfig = openAiConfig;
        this.okHttpClient = configuration.getOkHttpClient();
        this.factory = configuration.createRequestFactory();
    }

    @Override
    public ChatResponse call(Prompt prompt) throws Exception {
        return internalCall(null, null, prompt);
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
                .post(RequestBody.create(MediaType.parse(Constants.JSON_CONTENT_TYPE), requestString))
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
     * 
     * @param baseUrl optional base URL override (uses config if null)
     * @param apiKey optional API key override (uses config if null)
     * @param prompt the prompt
     * @return Flux of chat responses
     * @throws Exception if error occurs
     */
    private Flux<ChatResponse> internalStream(String baseUrl, String apiKey, Prompt prompt) throws Exception {
        if(baseUrl == null || baseUrl.isEmpty()) baseUrl = openAiConfig.getApiHost();
        if(apiKey == null || apiKey.isEmpty()) apiKey = openAiConfig.getApiKey();
        prompt.setStream(true);
        StreamOptions streamOptions = prompt.getStreamOptions();
        if(streamOptions == null){
            prompt.setStreamOptions(new StreamOptions(true));
        }

        final AtomicReference<Exception> errorRef = new AtomicReference<>();
        
        StreamingResponseHandler handler = new StreamingResponseHandler() {
            @Override
            protected void send() {
                // Collect streaming responses
            }
            
            @Override
            protected void error(Throwable t, Response response) {
                log.error("Streaming error", t);
                if (t instanceof Exception) {
                    errorRef.set((Exception) t);
                }
            }
        };

        ObjectMapper mapper = new ObjectMapper();
        String jsonString = mapper.writeValueAsString(prompt);

        Request request = new Request.Builder()
                .header("Authorization", "Bearer " + apiKey)
                .url(ValidateUtil.concatUrl(baseUrl, openAiConfig.getChatCompletionUrl()))
                .post(RequestBody.create(MediaType.parse(Constants.APPLICATION_JSON), jsonString))
                .build();

        factory.newEventSource(request, handler);
        handler.getCountDownLatch().await();
        
        if (errorRef.get() != null) {
            throw errorRef.get();
        }
        
        // Return empty flux for now - streaming implementation needs full reactive support
        // For full streaming support, consider using real reactive libraries like Project Reactor
        return Flux.empty();
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
                .post(RequestBody.create(MediaType.parse(Constants.APPLICATION_JSON), jsonString))
                .build();

        factory.newEventSource(request, handler);
        handler.getCountDownLatch().await();
    }
}
