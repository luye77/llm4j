package com.bobo.llm4j.integration;

import com.bobo.llm4j.chat.client.ChatClient;
import com.bobo.llm4j.chat.entity.Message;
import com.bobo.llm4j.config.Configuration;
import com.bobo.llm4j.config.QwenConfig;
import com.bobo.llm4j.memory.LayeredChatMemory;
import com.bobo.llm4j.memory.MessageChatMemoryAdvisor;
import com.bobo.llm4j.platform.qwen.chat.QwenChatModel;
import com.bobo.llm4j.platform.qwen.chat.QwenChatOptions;
import com.bobo.llm4j.rag.advisor.QuestionAnswerAdvisor;
import com.bobo.llm4j.rag.document.RagDocument;
import com.bobo.llm4j.rag.retrieval.DocumentRetriever;
import com.bobo.llm4j.tool.advisor.ToolCallingAdvisor;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


import java.util.Arrays;

/**
 * 全链路集成测试：Qwen 模型 + 工具调用 + RAG + 会话记忆
 * <p>
 * 测试框架完整功能链：
 * <ul>
 *   <li>工具调用 (Tool Calling)：模型可调用业务工具</li>
 *   <li>RAG：基于检索的问答增强</li>
 *   <li>会话记忆 (Memory)：跨轮次对话上下文</li>
 * </ul>
 * <p>
 * Advisor 执行顺序（按文档）：QuestionAnswerAdvisor → MessageChatMemoryAdvisor → ToolCallingAdvisor → ChatModelCallAdvisor
 */
@Slf4j
public class QwenFullChainIntegrationTest {

    private ChatClient chatClient;

    @Before
    public void setUp() {
        // 1. 配置 Qwen
        QwenConfig qwenConfig = QwenConfig.builder()
                .apiHost("https://dashscope.aliyuncs.com/")
                .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                .build();

        Configuration configuration = Configuration.builder()
                .qwenConfig(qwenConfig)
                .build();

        // 2. 创建 Qwen ChatModel
        QwenChatModel chatModel = new QwenChatModel(configuration);

        // 3. RAG：简单 DocumentRetriever（返回固定文档，无需 embedding）
        DocumentRetriever retriever = new SimpleDocumentRetriever(Arrays.asList(
                RagDocument.of("llm4j 是一个 Java LLM 客户端库，参考 Spring AI 设计，支持工具调用、RAG、会话记忆等功能。"),
                RagDocument.of("工具调用 (Tool Calling) 允许大模型在执行过程中调用业务函数，模型根据用户意图选择工具并传入参数。"),
                RagDocument.of("RAG (检索增强生成) 通过检索相关文档来增强模型回答的准确性。")
        ));
        QuestionAnswerAdvisor ragAdvisor = QuestionAnswerAdvisor.builder(retriever)
                .order(0)
                .build();

        // 4. 会话记忆
        LayeredChatMemory chatMemory = LayeredChatMemory.builder()
                .recentDays(2)
                .build();
        MessageChatMemoryAdvisor memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory)
                .conversationId("test-conversation")
                .order(1)
                .build();

        // 5. 工具调用
        ToolCallingAdvisor toolAdvisor = ToolCallingAdvisor.builder()
                .chatModel(chatModel)
                .tools(new WeatherTools(), new CalculatorTools())
                .maxIterations(5)
                .order(2)
                .build();

        // 6. 构建 ChatClient（Advisor 顺序：RAG → Memory → Tool）
        chatClient = ChatClient.builder(chatModel)
                .defaultSystem("你是一个有帮助的 AI 助手，可以查询天气、做数学计算，并能基于提供的上下文回答问题。")
                .defaultOptions(QwenChatOptions.builder()
                        .model("qwen-flash")
                        .temperature(0.7f)
                        .maxTokens(1000)
                        .build())
                .defaultAdvisors(ragAdvisor, memoryAdvisor, toolAdvisor)
                .build();
    }

    /**
     * 测试 1：工具调用 - 查询天气
     */
    @Test
    public void testToolCalling_Weather() throws Exception {
        log.info("=== 测试工具调用：天气查询 ===");

        String response = chatClient.prompt()
                .messages(Message.withUser("杭州今天天气怎么样？"))
                .call()
                .content();

        log.info("Response: {}", response);
        Assert.assertNotNull(response);
        Assert.assertFalse(response.isEmpty());
    }

    /**
     * 测试 2：RAG - 基于检索的问答
     */
    @Test
    public void testRag_QuestionAnswer() throws Exception {
        log.info("=== 测试 RAG：检索增强问答 ===");

        String response = chatClient.prompt()
                .messages(Message.withUser("llm4j 支持哪些功能？"))
                .call()
                .content();

        log.info("Response: {}", response);
        Assert.assertNotNull(response);
        Assert.assertFalse(response.isEmpty());
        // RAG 上下文应包含 llm4j 相关文档内容
        Assert.assertTrue("RAG 响应应包含检索到的文档关键词",
                response.contains("工具") || response.contains("RAG") || response.contains("记忆")
                );
    }

    /**
     * 测试 3：会话记忆 - 多轮对话
     */
    @Test
    public void testMemory_MultiTurnConversation() throws Exception {
        log.info("=== 测试会话记忆：多轮对话 ===");

        // 第一轮
        String response1 = chatClient.prompt()
                .messages(Message.withUser("我叫小明，是一名 Java 开发者"))
                .call()
                .content();
        log.info("Round 1 Response: {}", response1);

        // 第二轮：依赖记忆中的名字（使用同一会话 ID）
        String response2 = chatClient.prompt()
                .messages(Message.withUser("你还记得我叫什么吗？"))
                .call()
                .content();
        log.info("Round 2 Response: {}", response2);

        Assert.assertNotNull(response1);
        Assert.assertFalse(response1.isEmpty());
        Assert.assertNotNull(response2);
        Assert.assertFalse(response2.isEmpty());
    }

    /**
     * 测试 4：全链路 - 工具 + RAG + 记忆 组合
     */
    @Test
    public void testFullChain_Combined() throws Exception {
        log.info("=== 测试全链路：工具 + RAG + 记忆 ===");

        // 第一轮：RAG 问答
        String r1 = chatClient.prompt()
                .messages(Message.withUser("llm4j 的 RAG 是什么？"))
                .call()
                .content();
        log.info("Round 1 (RAG): {}", r1);

        // 第二轮：工具调用
        String r2 = chatClient.prompt()
                .messages(Message.withUser("北京天气如何？再帮我算一下 123 + 456"))
                .call()
                .content();
        log.info("Round 2 (Tool): {}", r2);

        Assert.assertNotNull(r1);
        Assert.assertFalse(r1.isEmpty());
        Assert.assertNotNull(r2);
        Assert.assertFalse(r2.isEmpty());
    }
}
