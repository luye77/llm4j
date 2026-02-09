package com.bobo.llm4j.memory;

import com.bobo.llm4j.chat.client.ChatClientRequest;
import com.bobo.llm4j.chat.client.ChatClientResponse;
import com.bobo.llm4j.platform.openai.chat.entity.ChatResponse;
import com.bobo.llm4j.platform.openai.chat.entity.Generation;
import com.bobo.llm4j.platform.openai.chat.entity.Message;
import com.bobo.llm4j.platform.openai.chat.enums.MessageType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MessageChatMemoryAdvisor 测试类
 * <p>
 * 参考 Spring AI 的 Advisor 模式测试
 * 测试 Advisor 的前置和后置处理逻辑
 *
 * @author bobo
 * @since 1.0.0
 */
public class MessageChatMemoryAdvisorTest {

    private ChatMemory chatMemory;
    private MessageChatMemoryAdvisor advisor;
    private String testConversationId;

    @Before
    public void setUp() {
        chatMemory = MessageWindowChatMemory.builder()
                .maxMessages(20)
                .build();
        testConversationId = "test-conversation-123";
        advisor = MessageChatMemoryAdvisor.builder(chatMemory)
                .conversationId(testConversationId)
                .build();
    }

    // ==================== Advisor 基础测试 ====================

    @Test
    public void testAdvisor_Name() {
        Assert.assertEquals("MessageChatMemoryAdvisor", advisor.getName());
    }

    @Test
    public void testAdvisor_Order() {
        MessageChatMemoryAdvisor customOrderAdvisor = MessageChatMemoryAdvisor.builder(chatMemory)
                .order(100)
                .build();
        Assert.assertEquals(100, customOrderAdvisor.getOrder());
    }

    @Test
    public void testAdvisor_DefaultOrder() {
        Assert.assertEquals(0, advisor.getOrder());
    }

    // ==================== Before 方法测试 ====================

    @Test
    public void testBefore_FirstRequest_NoHistory() {
        // 第一次请求，没有历史记录
        ChatClientRequest request = createRequest(
                Collections.singletonList(Message.withUser("Hello")),
                testConversationId
        );

        ChatClientRequest processedRequest = advisor.before(request, null);

        // 应该只有当前消息
        Assert.assertEquals(1, processedRequest.getMessages().size());
        Assert.assertEquals("Hello", processedRequest.getMessages().get(0).getContent().getText());

        // 用户消息应该已经保存到记忆中
        List<Message> memory = chatMemory.get(testConversationId);
        Assert.assertEquals(1, memory.size());
        Assert.assertEquals(MessageType.USER.getRole(), memory.get(0).getRole());
    }

    @Test
    public void testBefore_SecondRequest_WithHistory() {
        // 模拟第一轮对话已完成
        chatMemory.add(testConversationId, Arrays.asList(
                Message.withUser("Hello"),
                Message.withAssistant("Hi there!")
        ));

        // 第二次请求
        ChatClientRequest request = createRequest(
                Collections.singletonList(Message.withUser("How are you?")),
                testConversationId
        );

        ChatClientRequest processedRequest = advisor.before(request, null);

        // 应该包含历史消息 + 当前消息 = 3 条
        Assert.assertEquals(3, processedRequest.getMessages().size());
        Assert.assertEquals("Hello", processedRequest.getMessages().get(0).getContent().getText());
        Assert.assertEquals("Hi there!", processedRequest.getMessages().get(1).getContent().getText());
        Assert.assertEquals("How are you?", processedRequest.getMessages().get(2).getContent().getText());
    }

    @Test
    public void testBefore_SystemMessage_MovedToFirst() {
        // 已有历史消息
        chatMemory.add(testConversationId, Arrays.asList(
                Message.withUser("First question"),
                Message.withAssistant("First answer")
        ));

        // 新请求包含 SystemMessage 在中间
        ChatClientRequest request = createRequest(
                Arrays.asList(
                        Message.withUser("New question"),
                        Message.withSystem("You are a helpful assistant")
                ),
                testConversationId
        );

        ChatClientRequest processedRequest = advisor.before(request, null);

        // SystemMessage 应该在第一位
        List<Message> messages = processedRequest.getMessages();
        Assert.assertEquals(MessageType.SYSTEM.getRole(), messages.get(0).getRole());
        Assert.assertEquals("You are a helpful assistant", messages.get(0).getContent().getText());
    }

    @Test
    public void testBefore_MultipleUserMessages_SavesLastOne() {
        ChatClientRequest request = createRequest(
                Arrays.asList(
                        Message.withSystem("System prompt"),
                        Message.withUser("Question 1"),
                        Message.withUser("Question 2")
                ),
                testConversationId
        );

        advisor.before(request, null);

        // 应该只保存最后一条用户消息
        List<Message> memory = chatMemory.get(testConversationId);
        Assert.assertEquals(1, memory.size());
        Assert.assertEquals("Question 2", memory.get(0).getContent().getText());
    }

    @Test
    public void testBefore_UseDefaultConversationId() {
        // 不在上下文中指定 conversationId，应该使用默认的
        ChatClientRequest request = new ChatClientRequest(
                Collections.singletonList(Message.withUser("Test")),
                null, null, null, new HashMap<>(), null
        );

        ChatClientRequest processedRequest = advisor.before(request, null);

        // 消息应该保存到默认的 conversationId
        List<Message> memory = chatMemory.get(testConversationId);
        Assert.assertEquals(1, memory.size());
    }

    @Test
    public void testBefore_UseContextConversationId() {
        String customConversationId = "custom-conv-id";
        Map<String, Object> context = new HashMap<>();
        context.put(ChatMemory.CONVERSATION_ID, customConversationId);

        ChatClientRequest request = new ChatClientRequest(
                Collections.singletonList(Message.withUser("Test with custom ID")),
                null, null, null, context, null
        );

        // 使用默认 conversationId 构建的 advisor
        MessageChatMemoryAdvisor defaultAdvisor = MessageChatMemoryAdvisor.builder(chatMemory)
                .conversationId("default-id")
                .build();

        defaultAdvisor.before(request, null);

        // 消息应该保存到自定义的 conversationId
        List<Message> memory = chatMemory.get(customConversationId);
        Assert.assertEquals(1, memory.size());
        Assert.assertEquals("Test with custom ID", memory.get(0).getContent().getText());

        // 默认 ID 应该没有消息
        Assert.assertEquals(0, chatMemory.get("default-id").size());
    }

    // ==================== After 方法测试 ====================

    @Test
    public void testAfter_SavesAssistantMessage() {
        ChatClientResponse response = createResponse(
                Collections.singletonList(
                        createGeneration(Message.withAssistant("This is the AI response"))
                ),
                testConversationId
        );

        advisor.after(response, null);

        // AI 回复应该保存到记忆中
        List<Message> memory = chatMemory.get(testConversationId);
        Assert.assertEquals(1, memory.size());
        Assert.assertEquals(MessageType.ASSISTANT.getRole(), memory.get(0).getRole());
        Assert.assertEquals("This is the AI response", memory.get(0).getContent().getText());
    }

    @Test
    public void testAfter_SavesMultipleGenerations() {
        ChatClientResponse response = createResponse(
                Arrays.asList(
                        createGeneration(Message.withAssistant("Response 1")),
                        createGeneration(Message.withAssistant("Response 2"))
                ),
                testConversationId
        );

        advisor.after(response, null);

        List<Message> memory = chatMemory.get(testConversationId);
        Assert.assertEquals(2, memory.size());
        Assert.assertEquals("Response 1", memory.get(0).getContent().getText());
        Assert.assertEquals("Response 2", memory.get(1).getContent().getText());
    }

    @Test
    public void testAfter_NullResponse_DoesNothing() {
        int sizeBefore = chatMemory.get(testConversationId).size();
        advisor.after(null, null);
        int sizeAfter = chatMemory.get(testConversationId).size();
        Assert.assertEquals(sizeBefore, sizeAfter);
    }

    @Test
    public void testAfter_EmptyGenerations_DoesNothing() {
        ChatClientResponse response = createResponse(
                Collections.emptyList(),
                testConversationId
        );

        advisor.after(response, null);

        Assert.assertEquals(0, chatMemory.get(testConversationId).size());
    }

    // ==================== 完整流程测试 ====================

    @Test
    public void testFullFlow_BeforeAndAfter() {
        // 1. 第一轮对话
        ChatClientRequest request1 = createRequest(
                Collections.singletonList(Message.withUser("What is AI?")),
                testConversationId
        );

        ChatClientRequest processedRequest1 = advisor.before(request1, null);
        Assert.assertEquals(1, processedRequest1.getMessages().size());

        ChatClientResponse response1 = createResponse(
                Collections.singletonList(
                        createGeneration(Message.withAssistant("AI is Artificial Intelligence"))
                ),
                testConversationId
        );

        advisor.after(response1, null);

        // 验证第一轮对话已保存
        List<Message> memoryAfterRound1 = chatMemory.get(testConversationId);
        Assert.assertEquals(2, memoryAfterRound1.size()); // user + assistant

        // 2. 第二轮对话
        ChatClientRequest request2 = createRequest(
                Collections.singletonList(Message.withUser("Tell me more")),
                testConversationId
        );

        ChatClientRequest processedRequest2 = advisor.before(request2, null);
        // 应该包含历史 (2) + 当前 (1) = 3
        Assert.assertEquals(3, processedRequest2.getMessages().size());

        ChatClientResponse response2 = createResponse(
                Collections.singletonList(
                        createGeneration(Message.withAssistant("AI includes machine learning"))
                ),
                testConversationId
        );

        advisor.after(response2, null);

        // 验证两轮对话都已保存
        List<Message> memoryAfterRound2 = chatMemory.get(testConversationId);
        Assert.assertEquals(4, memoryAfterRound2.size()); // 2 user + 2 assistant
    }

    @Test
    public void testFullFlow_WithSystemMessage() {
        // 包含系统消息的完整流程
        ChatClientRequest request = createRequest(
                Arrays.asList(
                        Message.withSystem("You are a helpful assistant"),
                        Message.withUser("Hello")
                ),
                testConversationId
        );

        ChatClientRequest processedRequest = advisor.before(request, null);

        // SystemMessage 应该在第一位
        Assert.assertEquals(MessageType.SYSTEM.getRole(), 
                processedRequest.getMessages().get(0).getRole());

        ChatClientResponse response = createResponse(
                Collections.singletonList(
                        createGeneration(Message.withAssistant("Hi!"))
                ),
                testConversationId
        );

        advisor.after(response, null);

        // 验证只有 user 和 assistant 被保存（SystemMessage 不保存）
        List<Message> memory = chatMemory.get(testConversationId);
        Assert.assertEquals(2, memory.size());
        Assert.assertEquals(MessageType.USER.getRole(), memory.get(0).getRole());
        Assert.assertEquals(MessageType.ASSISTANT.getRole(), memory.get(1).getRole());
    }

    // ==================== 边界情况测试 ====================

    @Test(expected = IllegalArgumentException.class)
    public void testBuilder_NullChatMemory() {
        MessageChatMemoryAdvisor.builder(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilder_EmptyConversationId() {
        MessageChatMemoryAdvisor.builder(chatMemory)
                .conversationId("")
                .build();
    }

    @Test
    public void testBefore_EmptyMessageList() {
        ChatClientRequest request = createRequest(
                Collections.emptyList(),
                testConversationId
        );

        ChatClientRequest processedRequest = advisor.before(request, null);
        Assert.assertEquals(0, processedRequest.getMessages().size());
    }

    @Test
    public void testBefore_NullMessageList() {
        ChatClientRequest request = new ChatClientRequest(
                null, null, null, null,
                createContext(testConversationId), null
        );

        ChatClientRequest processedRequest = advisor.before(request, null);
        Assert.assertNotNull(processedRequest.getMessages());
    }

    // ==================== 辅助方法 ====================

    private ChatClientRequest createRequest(List<Message> messages, String conversationId) {
        return new ChatClientRequest(
                messages,
                null,
                null,
                null,
                createContext(conversationId),
                null
        );
    }

    private ChatClientResponse createResponse(List<Generation> generations, String conversationId) {
        ChatResponse chatResponse = new ChatResponse();
        chatResponse.setGenerations(generations);

        return ChatClientResponse.builder()
                .chatResponse(chatResponse)
                .metadata(createContext(conversationId))
                .build();
    }

    private Generation createGeneration(Message message) {
        Generation generation = new Generation();
        generation.setMessage(message);
        return generation;
    }

    private Map<String, Object> createContext(String conversationId) {
        Map<String, Object> context = new HashMap<>();
        if (conversationId != null) {
            context.put(ChatMemory.CONVERSATION_ID, conversationId);
        }
        return context;
    }
}
