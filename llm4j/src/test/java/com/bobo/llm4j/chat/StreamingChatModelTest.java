package com.bobo.llm4j.chat;

import com.bobo.llm4j.chat.client.ChatOptions;
import com.bobo.llm4j.chat.entity.ChatResponse;
import com.bobo.llm4j.chat.entity.Message;
import com.bobo.llm4j.chat.entity.Prompt;
import com.bobo.llm4j.chat.model.ChatModel;
import com.bobo.llm4j.config.Configuration;
import com.bobo.llm4j.config.OpenAiConfig;
import com.bobo.llm4j.http.Flux;
import com.bobo.llm4j.platform.openai.chat.OpenAiChatModel;
import com.bobo.llm4j.platform.openai.chat.OpenAiChatOptions;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * StreamingChatModel 测试类
 * <p>
 * 演示如何使用新的流式API和ChatOptions
 * </p>
 */
@Slf4j
@Disabled("需要配置有效的 OpenAI API Key")
public class StreamingChatModelTest {

    private ChatModel chatModel;
    private OpenAiChatOptions defaultOptions;

    @BeforeEach
    void setUp() {
        // 配置 OpenAI
        OpenAiConfig openAiConfig = OpenAiConfig.builder()
                .apiHost("https://api.openai.com")
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .build();

        Configuration configuration = Configuration.builder()
                .openAiConfig(openAiConfig)
                .build();

        // 配置默认选项
        defaultOptions = OpenAiChatOptions.builder()
                .model("gpt-3.5-turbo")
                .temperature(0.7f)
                .maxTokens(500)
                .build();

        chatModel = new OpenAiChatModel(configuration, defaultOptions);
    }

    /**
     * 测试基本的同步调用
     */
    @Test
    void testSynchronousCall() throws Exception {
        log.info("=== Test Synchronous Call ===");

        Message userMessage = Message.builder()
                .role("user")
                .content(Message.Content.builder()
                        .text("Say hello in Chinese")
                        .build())
                .build();

        Prompt prompt = new Prompt(List.of(userMessage));
        ChatResponse response = chatModel.call(prompt);

        log.info("Response: {}", response.getGenerations().get(0).getMessage().getContent().getText());
        log.info("Usage: {}", response.getUsage());
    }

    /**
     * 测试运行时覆盖选项
     */
    @Test
    void testRuntimeOptionsOverride() throws Exception {
        log.info("=== Test Runtime Options Override ===");

        Message userMessage = Message.builder()
                .role("user")
                .content(Message.Content.builder()
                        .text("Write a creative poem")
                        .build())
                .build();

        // 运行时覆盖选项 - 使用更高的温度以增加创造性
        OpenAiChatOptions runtimeOptions = OpenAiChatOptions.builder()
                .temperature(1.2f)  // 覆盖默认的 0.7
                .maxTokens(1000)    // 覆盖默认的 500
                .build();

        Prompt prompt = new Prompt(List.of(userMessage), runtimeOptions);
        ChatResponse response = chatModel.call(prompt);

        log.info("Response: {}", response.getGenerations().get(0).getMessage().getContent().getText());
        log.info("Model used: {}", response.getModel());
    }

    /**
     * 测试流式响应
     */
    @Test
    void testStreamingResponse() throws Exception {
        log.info("=== Test Streaming Response ===");

        Message userMessage = Message.builder()
                .role("user")
                .content(Message.Content.builder()
                        .text("Count from 1 to 10 slowly")
                        .build())
                .build();

        Prompt prompt = new Prompt(List.of(userMessage));
        Flux<ChatResponse> stream = chatModel.stream(prompt);

        CountDownLatch latch = new CountDownLatch(1);
        StringBuilder fullResponse = new StringBuilder();

        stream.subscribe(
                response -> {
                    // 处理每个数据块
                    if (response.getGenerations() != null && !response.getGenerations().isEmpty()) {
                        var delta = response.getGenerations().get(0).getDelta();
                        if (delta != null && delta.getContent() != null && delta.getContent().getText() != null) {
                            String text = delta.getContent().getText();
                            fullResponse.append(text);
                            System.out.print(text);  // 实时打印
                        }
                    }
                },
                error -> {
                    // 处理错误
                    log.error("Stream error", error);
                    latch.countDown();
                },
                () -> {
                    // 流完成
                    System.out.println("\n\n=== Stream completed ===");
                    log.info("Full response: {}", fullResponse.toString());
                    latch.countDown();
                }
        );

        // 等待流完成（最多30秒）
        latch.await(30, TimeUnit.SECONDS);
    }

    /**
     * 测试 ChatOptions 合并
     */
    @Test
    void testOptionsMetrge() {
        log.info("=== Test Options Merge ===");

        OpenAiChatOptions defaults = OpenAiChatOptions.builder()
                .model("gpt-3.5-turbo")
                .temperature(0.7f)
                .maxTokens(2000)
                .frequencyPenalty(0.0f)
                .build();

        OpenAiChatOptions runtime = OpenAiChatOptions.builder()
                .temperature(0.9f)   // 覆盖
                .model("gpt-4")      // 覆盖
                .seed(42)            // 新增
                .build();

        ChatOptions merged = defaults.merge(runtime);

        log.info("Merged options:");
        log.info("  Model: {}", merged.getModel());              // gpt-4 (runtime)
        log.info("  Temperature: {}", merged.getTemperature());  // 0.9 (runtime)
        log.info("  MaxTokens: {}", merged.getMaxTokens());      // 2000 (defaults)
        log.info("  Frequency Penalty: {}", merged.getFrequencyPenalty()); // 0.0 (defaults)
        
        OpenAiChatOptions mergedOpenAi = (OpenAiChatOptions) merged;
        log.info("  Seed: {}", mergedOpenAi.getSeed());          // 42 (runtime)
    }

    /**
     * 测试 JSON 模式
     */
    @Test
    void testJsonMode() throws Exception {
        log.info("=== Test JSON Mode ===");

        Message systemMessage = Message.builder()
                .role("system")
                .content(Message.Content.builder()
                        .text("You are a helpful assistant that outputs JSON")
                        .build())
                .build();

        Message userMessage = Message.builder()
                .role("user")
                .content(Message.Content.builder()
                        .text("Generate a JSON object with name and age fields for a fictional person")
                        .build())
                .build();

        OpenAiChatOptions jsonOptions = OpenAiChatOptions.builder()
                .model("gpt-3.5-turbo-1106")  // 支持 JSON 模式
                .responseFormat(OpenAiChatOptions.ResponseFormat.jsonObject())
                .build();

        Prompt prompt = new Prompt(List.of(systemMessage, userMessage), jsonOptions);
        ChatResponse response = chatModel.call(prompt);

        String jsonResponse = response.getGenerations().get(0).getMessage().getContent().getText();
        log.info("JSON Response: {}", jsonResponse);
    }

    /**
     * 测试便捷工厂方法
     */
    @Test
    void testConvenienceFactories() {
        log.info("=== Test Convenience Factories ===");

        // 使用预定义的配置
        OpenAiChatOptions gpt4Options = OpenAiChatOptions.gpt4().build();
        log.info("GPT-4 defaults: model={}, temp={}, maxTokens={}",
                gpt4Options.getModel(), gpt4Options.getTemperature(), gpt4Options.getMaxTokens());

        OpenAiChatOptions gpt4TurboOptions = OpenAiChatOptions.gpt4Turbo().build();
        log.info("GPT-4 Turbo defaults: model={}, temp={}, maxTokens={}",
                gpt4TurboOptions.getModel(), gpt4TurboOptions.getTemperature(), gpt4TurboOptions.getMaxTokens());

        OpenAiChatOptions defaultsOptions = OpenAiChatOptions.defaults().build();
        log.info("Defaults: model={}, temp={}, maxTokens={}",
                defaultsOptions.getModel(), defaultsOptions.getTemperature(), defaultsOptions.getMaxTokens());
    }
}
