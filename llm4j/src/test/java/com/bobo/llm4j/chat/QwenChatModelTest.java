package com.bobo.llm4j.chat;

import com.bobo.llm4j.chat.client.ChatOptions;
import com.bobo.llm4j.chat.entity.ChatResponse;
import com.bobo.llm4j.chat.entity.Media;
import com.bobo.llm4j.chat.entity.Message;
import com.bobo.llm4j.chat.entity.Prompt;
import com.bobo.llm4j.chat.model.ChatModel;
import com.bobo.llm4j.config.Configuration;
import com.bobo.llm4j.config.QwenConfig;
import com.bobo.llm4j.http.Flux;
import com.bobo.llm4j.platform.qwen.chat.QwenChatModel;
import com.bobo.llm4j.platform.qwen.chat.QwenChatOptions;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * QwenChatModel 测试类
 * <p>
 * 演示如何使用千问(Qwen)模型的流式API和ChatOptions
 * </p>
 */
@Slf4j
public class QwenChatModelTest {

    private ChatModel chatModel;
    private QwenChatOptions defaultOptions;

    @Before
    public void setUp() {
        // 配置千问
        QwenConfig qwenConfig = QwenConfig.builder()
                .apiHost("https://dashscope.aliyuncs.com/")
                .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                .build();

        Configuration configuration = Configuration.builder()
                .qwenConfig(qwenConfig)
                .build();

        // 配置默认选项
        defaultOptions = QwenChatOptions.builder()
                .model("qwen-turbo")
                .temperature(0.7f)
                .maxTokens(500)
                .build();

        chatModel = new QwenChatModel(configuration);
    }

    /**
     * 测试基本的同步调用
     */
    @Test
    public void testSynchronousCall() throws Exception {
        log.info("=== Test Qwen Synchronous Call ===");

        Message userMessage = Message.builder()
                .role("user")
                .content(Media.ofText("用中文说你好"))
                .build();

        Prompt prompt = Prompt.builder()
                .message(userMessage)
                .model(defaultOptions.getModel())
                .temperature(defaultOptions.getTemperature())
                .maxCompletionTokens(defaultOptions.getMaxTokens())
                .build();
        
        ChatResponse response = chatModel.call(prompt);

        log.info("Response: {}", response.getGenerations().get(0).getMessage().getContent().getText());
        log.info("Usage: {}", response.getUsage());
    }

    /**
     * 测试运行时覆盖选项
     */
    @Test
    public void testRuntimeOptionsOverride() throws Exception {
        log.info("=== Test Qwen Runtime Options Override ===");

        Message userMessage = Message.builder()
                .role("user")
                .content(Media.ofText("写一首关于春天的诗"))
                .build();

        // 运行时覆盖选项 - 使用更高的温度以增加创造性
        Prompt prompt = Prompt.builder()
                .message(userMessage)
                .model("qwen-plus")  // 使用更强大的模型
                .temperature(1.0f)   // 覆盖默认的 0.7
                .maxCompletionTokens(1000)  // 覆盖默认的 500
                .build();
        
        ChatResponse response = chatModel.call(prompt);

        log.info("Response: {}", response.getGenerations().get(0).getMessage().getContent().getText());
        log.info("Model used: {}", response.getModel());
    }

    /**
     * 测试流式响应
     */
    @Test
    public void testStreamingResponse() throws Exception {
        log.info("=== Test Qwen Streaming Response ===");

        Message userMessage = Message.builder()
                .role("user")
                .content(Media.ofText("从1数到10"))
                .build();

        Prompt prompt = Prompt.builder()
                .message(userMessage)
                .model(defaultOptions.getModel())
                .temperature(defaultOptions.getTemperature())
                .maxCompletionTokens(defaultOptions.getMaxTokens())
                .build();
        
        Flux<ChatResponse> stream = chatModel.stream(prompt);

        CountDownLatch latch = new CountDownLatch(1);
        StringBuilder fullResponse = new StringBuilder();

        stream.subscribe(
                response -> {
                    // 处理每个数据块
                    if (response.getGenerations() != null && !response.getGenerations().isEmpty()) {
                        Message delta = response.getGenerations().get(0).getDelta();
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
    public void testOptionsMerge() {
        log.info("=== Test Qwen Options Merge ===");

        QwenChatOptions defaults = QwenChatOptions.builder()
                .model("qwen-turbo")
                .temperature(0.7f)
                .maxTokens(2000)
                .frequencyPenalty(0.0f)
                .build();

        QwenChatOptions runtime = QwenChatOptions.builder()
                .temperature(0.9f)   // 覆盖
                .model("qwen-max")   // 覆盖
                .seed(42)            // 新增
                .build();

        ChatOptions merged = defaults.merge(runtime);

        log.info("Merged options:");
        log.info("  Model: {}", merged.getModel());              // qwen-max (runtime)
        log.info("  Temperature: {}", merged.getTemperature());  // 0.9 (runtime)
        log.info("  MaxTokens: {}", merged.getMaxTokens());      // 2000 (defaults)
        log.info("  Frequency Penalty: {}", merged.getFrequencyPenalty()); // 0.0 (defaults)
        
        QwenChatOptions mergedQwen = (QwenChatOptions) merged;
        log.info("  Seed: {}", mergedQwen.getSeed());            // 42 (runtime)
    }

    /**
     * 测试 JSON 模式
     */
    @Test
    public void testJsonMode() throws Exception {
        log.info("=== Test Qwen JSON Mode ===");

        Message systemMessage = Message.builder()
                .role("system")
                .content(Media.ofText("你是一个有帮助的助手，以JSON格式输出"))
                .build();

        Message userMessage = Message.builder()
                .role("user")
                .content(Media.ofText("生成一个包含姓名和年龄字段的虚拟人物JSON对象"))
                .build();

        Prompt prompt = Prompt.builder()
                .message(systemMessage)
                .message(userMessage)
                .model("qwen-turbo")
                .responseFormat(QwenChatOptions.ResponseFormat.jsonObject())
                .build();
        
        ChatResponse response = chatModel.call(prompt);

        String jsonResponse = response.getGenerations().get(0).getMessage().getContent().getText();
        log.info("JSON Response: {}", jsonResponse);
    }

    /**
     * 测试便捷工厂方法
     */
    @Test
    public void testConvenienceFactories() {
        log.info("=== Test Qwen Convenience Factories ===");

        // 使用预定义的配置
        QwenChatOptions qwenTurboOptions = QwenChatOptions.qwenTurbo().build();
        log.info("Qwen-Turbo defaults: model={}, temp={}, maxTokens={}",
                qwenTurboOptions.getModel(), qwenTurboOptions.getTemperature(), qwenTurboOptions.getMaxTokens());

        QwenChatOptions qwenPlusOptions = QwenChatOptions.qwenPlus().build();
        log.info("Qwen-Plus defaults: model={}, temp={}, maxTokens={}",
                qwenPlusOptions.getModel(), qwenPlusOptions.getTemperature(), qwenPlusOptions.getMaxTokens());

        QwenChatOptions qwenMaxOptions = QwenChatOptions.qwenMax().build();
        log.info("Qwen-Max defaults: model={}, temp={}, maxTokens={}",
                qwenMaxOptions.getModel(), qwenMaxOptions.getTemperature(), qwenMaxOptions.getMaxTokens());

        QwenChatOptions defaultsOptions = QwenChatOptions.defaults().build();
        log.info("Defaults: model={}, temp={}, maxTokens={}",
                defaultsOptions.getModel(), defaultsOptions.getTemperature(), defaultsOptions.getMaxTokens());
    }

    /**
     * 测试搜索增强功能
     */
    @Test
    public void testEnableSearch() throws Exception {
        log.info("=== Test Qwen Enable Search ===");

        Message userMessage = Message.builder()
                .role("user")
                .content(Media.ofText("2024年的奥运会在哪里举办？"))
                .build();

        Prompt prompt = Prompt.builder()
                .message(userMessage)
                .model("qwen-plus")
                .temperature(0.7f)
                .maxCompletionTokens(500)
                .build();
        
        // 注意：Prompt 类需要支持 enableSearch 字段才能使用此功能
        ChatResponse response = chatModel.call(prompt);

        log.info("Response with search: {}", response.getGenerations().get(0).getMessage().getContent().getText());
    }
}
