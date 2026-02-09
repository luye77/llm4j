package com.bobo.llm4j.memory;

import com.bobo.llm4j.platform.openai.chat.entity.Message;
import com.bobo.llm4j.platform.openai.chat.enums.MessageType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 重构后的会话记忆测试（参考 Spring AI 设计）
 * <p>
 * 测试内容：
 * 1. ChatMemoryRepository - 存储层测试
 * 2. MessageWindowChatMemory - 滑动窗口策略测试
 * 3. SystemMessage 特殊处理测试
 * 4. MessageChatMemoryAdvisor - Advisor 模式测试
 *
 * @author bobo
 * @since 1.0.0
 */
public class ChatMemoryTest {

    private ChatMemoryRepository repository;
    private ChatMemory chatMemory;

    @Before
    public void setUp() {
        repository = new InMemoryChatMemoryRepository();
        chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(20)
                .build();
    }

    // ==================== Repository 层测试 ====================

    @Test
    public void testInMemoryChatMemoryRepository_SaveAndFind() {
        String conversationId = "test-conv-1";
        List<Message> messages = Arrays.asList(
                Message.withUser("hello"),
                Message.withAssistant("hi there")
        );

        repository.saveAll(conversationId, messages);
        List<Message> retrieved = repository.findByConversationId(conversationId);

        Assert.assertEquals(2, retrieved.size());
        Assert.assertEquals("user", retrieved.get(0).getRole());
        Assert.assertEquals("assistant", retrieved.get(1).getRole());
    }

    @Test
    public void testInMemoryChatMemoryRepository_Delete() {
        String conversationId = "test-conv-2";
        repository.saveAll(conversationId, Collections.singletonList(Message.withUser("test")));

        Assert.assertEquals(1, repository.findByConversationId(conversationId).size());

        repository.deleteByConversationId(conversationId);
        Assert.assertEquals(0, repository.findByConversationId(conversationId).size());
    }

    @Test
    public void testInMemoryChatMemoryRepository_FindConversationIds() {
        repository.saveAll("conv1", Collections.singletonList(Message.withUser("msg1")));
        repository.saveAll("conv2", Collections.singletonList(Message.withUser("msg2")));

        List<String> ids = repository.findConversationIds();
        Assert.assertTrue(ids.contains("conv1"));
        Assert.assertTrue(ids.contains("conv2"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInMemoryChatMemoryRepository_NullConversationId() {
        repository.saveAll(null, Collections.singletonList(Message.withUser("test")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInMemoryChatMemoryRepository_EmptyConversationId() {
        repository.saveAll("", Collections.singletonList(Message.withUser("test")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInMemoryChatMemoryRepository_NullMessages() {
        repository.saveAll("conv", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInMemoryChatMemoryRepository_MessagesContainNull() {
        repository.saveAll("conv", Arrays.asList(Message.withUser("test"), null));
    }

    // ==================== ChatMemory 基础测试 ====================

    @Test
    public void testChatMemory_AddSingleMessage() {
        String conversationId = "single-msg-test";
        chatMemory.add(conversationId, Message.withUser("Hello"));

        List<Message> messages = chatMemory.get(conversationId);
        Assert.assertEquals(1, messages.size());
        Assert.assertEquals("Hello", messages.get(0).getContent().getText());
    }

    @Test
    public void testChatMemory_AddMultipleMessages() {
        String conversationId = "multi-msg-test";
        chatMemory.add(conversationId, Arrays.asList(
                Message.withUser("Question 1"),
                Message.withAssistant("Answer 1")
        ));

        List<Message> messages = chatMemory.get(conversationId);
        Assert.assertEquals(2, messages.size());
    }

    @Test
    public void testChatMemory_Clear() {
        String conversationId = "clear-test";
        chatMemory.add(conversationId, Message.withUser("Test"));
        Assert.assertEquals(1, chatMemory.get(conversationId).size());

        chatMemory.clear(conversationId);
        Assert.assertEquals(0, chatMemory.get(conversationId).size());
    }

    // ==================== MessageWindowChatMemory 窗口测试 ====================

    @Test
    public void testMessageWindowChatMemory_EnforcesMaxMessages() {
        ChatMemory windowMemory = new MessageWindowChatMemory(3);
        String conversationId = "window-test";

        // 添加 4 条消息
        windowMemory.add(conversationId, Arrays.asList(
                Message.withUser("msg1"),
                Message.withUser("msg2"),
                Message.withUser("msg3")
        ));
        windowMemory.add(conversationId, Message.withUser("msg4"));

        List<Message> messages = windowMemory.get(conversationId);
        Assert.assertEquals(3, messages.size());
        // 最旧的 msg1 应该被删除
        Assert.assertEquals("msg2", messages.get(0).getContent().getText());
        Assert.assertEquals("msg4", messages.get(2).getContent().getText());
    }

    @Test
    public void testMessageWindowChatMemory_PreservesSystemMessage() {
        ChatMemory windowMemory = new MessageWindowChatMemory(3);
        String conversationId = "system-preserve-test";

        // 添加 SystemMessage + 3 条用户消息（超过限制）
        windowMemory.add(conversationId, Arrays.asList(
                Message.withSystem("You are a helpful assistant"),
                Message.withUser("msg1"),
                Message.withUser("msg2"),
                Message.withUser("msg3")
        ));

        List<Message> messages = windowMemory.get(conversationId);
        Assert.assertEquals(3, messages.size());
        
        // SystemMessage 应该被保留
        Assert.assertEquals(MessageType.SYSTEM.getRole(), messages.get(0).getRole());
        // msg1 被删除，保留 msg2 和 msg3
        Assert.assertEquals("msg2", messages.get(1).getContent().getText());
        Assert.assertEquals("msg3", messages.get(2).getContent().getText());
    }

    @Test
    public void testMessageWindowChatMemory_ReplacesOldSystemMessage() {
        ChatMemory windowMemory = new MessageWindowChatMemory(10);
        String conversationId = "system-replace-test";

        // 添加旧的 SystemMessage
        windowMemory.add(conversationId, Arrays.asList(
                Message.withSystem("Old system prompt"),
                Message.withUser("user message 1")
        ));

        // 添加新的 SystemMessage
        windowMemory.add(conversationId, Arrays.asList(
                Message.withSystem("New system prompt"),
                Message.withUser("user message 2")
        ));

        List<Message> messages = windowMemory.get(conversationId);
        
        // 应该有 3 条消息：新 SystemMessage + 2 条用户消息
        Assert.assertEquals(3, messages.size());
        
        // 验证只有新的 SystemMessage
        long systemMessageCount = messages.stream()
                .filter(m -> MessageType.SYSTEM.getRole().equals(m.getRole()))
                .count();
        Assert.assertEquals(1, systemMessageCount);
        Assert.assertEquals("New system prompt", messages.get(0).getContent().getText());
    }

    @Test
    public void testMessageWindowChatMemory_MultipleSystemMessages() {
        ChatMemory windowMemory = new MessageWindowChatMemory(10);
        String conversationId = "multi-system-test";

        // 第一次添加
        windowMemory.add(conversationId, Arrays.asList(
                Message.withSystem("System 1"),
                Message.withUser("User 1"),
                Message.withAssistant("Assistant 1")
        ));

        // 第二次添加，包含新的 SystemMessage
        windowMemory.add(conversationId, Arrays.asList(
                Message.withSystem("System 2"),
                Message.withUser("User 2")
        ));

        List<Message> messages = windowMemory.get(conversationId);
        
        // 应该只保留最新的 SystemMessage
        long systemCount = messages.stream()
                .filter(m -> MessageType.SYSTEM.getRole().equals(m.getRole()))
                .count();
        Assert.assertEquals(1, systemCount);
        Assert.assertEquals("System 2", messages.get(0).getContent().getText());
    }

    // ==================== MessageWindowChatMemory Builder 测试 ====================

    @Test
    public void testMessageWindowChatMemory_Builder() {
        ChatMemory memory = MessageWindowChatMemory.builder()
                .maxMessages(5)
                .build();

        String conversationId = "builder-test";
        for (int i = 0; i < 7; i++) {
            memory.add(conversationId, Message.withUser("msg" + i));
        }

        List<Message> messages = memory.get(conversationId);
        Assert.assertEquals(5, messages.size());
        Assert.assertEquals("msg2", messages.get(0).getContent().getText());
    }

    @Test
    public void testMessageWindowChatMemory_BuilderWithCustomRepository() {
        ChatMemoryRepository customRepo = new InMemoryChatMemoryRepository();
        ChatMemory memory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(customRepo)
                .maxMessages(3)
                .build();

        String conversationId = "custom-repo-test";
        memory.add(conversationId, Message.withUser("test"));

        Assert.assertEquals(1, memory.get(conversationId).size());
        Assert.assertEquals(1, customRepo.findByConversationId(conversationId).size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMessageWindowChatMemory_InvalidMaxMessages() {
        new MessageWindowChatMemory(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMessageWindowChatMemory_NegativeMaxMessages() {
        new MessageWindowChatMemory(-1);
    }

    // ==================== 边界条件测试 ====================

    @Test
    public void testMessageWindowChatMemory_ExactlyMaxMessages() {
        ChatMemory windowMemory = new MessageWindowChatMemory(3);
        String conversationId = "exact-max-test";

        windowMemory.add(conversationId, Arrays.asList(
                Message.withUser("msg1"),
                Message.withUser("msg2"),
                Message.withUser("msg3")
        ));

        List<Message> messages = windowMemory.get(conversationId);
        Assert.assertEquals(3, messages.size());
    }

    @Test
    public void testMessageWindowChatMemory_EmptyMessageList() {
        String conversationId = "empty-list-test";
        chatMemory.add(conversationId, Collections.emptyList());

        List<Message> messages = chatMemory.get(conversationId);
        Assert.assertEquals(0, messages.size());
    }

    @Test
    public void testMessageWindowChatMemory_NonExistentConversation() {
        List<Message> messages = chatMemory.get("non-existent");
        Assert.assertNotNull(messages);
        Assert.assertEquals(0, messages.size());
    }

    // ==================== 并发安全测试 ====================

    @Test
    public void testInMemoryChatMemoryRepository_ConcurrentAccess() throws InterruptedException {
        String conversationId = "concurrent-test";
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                repository.saveAll(conversationId + index, 
                        Collections.singletonList(Message.withUser("msg" + index)));
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        List<String> ids = repository.findConversationIds();
        Assert.assertEquals(threadCount, ids.size());
    }

    // ==================== 实际使用场景测试 ====================

    @Test
    public void testRealWorldScenario_MultiTurnConversation() {
        ChatMemory windowMemory = new MessageWindowChatMemory(10);
        String conversationId = "real-world-test";

        // 第一轮对话
        windowMemory.add(conversationId, Message.withSystem("You are a helpful assistant"));
        windowMemory.add(conversationId, Message.withUser("What is Java?"));
        windowMemory.add(conversationId, Message.withAssistant("Java is a programming language."));

        // 第二轮对话
        windowMemory.add(conversationId, Message.withUser("Is it popular?"));
        windowMemory.add(conversationId, Message.withAssistant("Yes, very popular."));

        // 第三轮对话
        windowMemory.add(conversationId, Message.withUser("Tell me more"));
        windowMemory.add(conversationId, Message.withAssistant("Java is used for enterprise applications."));

        List<Message> messages = windowMemory.get(conversationId);
        
        // 应该有 7 条消息：1 system + 3 user + 3 assistant
        Assert.assertEquals(7, messages.size());
        Assert.assertEquals(MessageType.SYSTEM.getRole(), messages.get(0).getRole());
        Assert.assertEquals(MessageType.ASSISTANT.getRole(), messages.get(messages.size() - 1).getRole());
    }

    @Test
    public void testRealWorldScenario_SystemPromptUpdate() {
        ChatMemory windowMemory = new MessageWindowChatMemory(10);
        String conversationId = "prompt-update-test";

        // 初始系统提示
        windowMemory.add(conversationId, Message.withSystem("You are a general assistant"));
        windowMemory.add(conversationId, Message.withUser("Hello"));
        windowMemory.add(conversationId, Message.withAssistant("Hi!"));

        // 更新系统提示（例如：用户改变了对话模式）
        windowMemory.add(conversationId, Message.withSystem("You are a code expert"));
        windowMemory.add(conversationId, Message.withUser("Write code"));
        windowMemory.add(conversationId, Message.withAssistant("Sure!"));

        List<Message> messages = windowMemory.get(conversationId);
        
        // 应该只有最新的系统提示
        Assert.assertEquals(MessageType.SYSTEM.getRole(), messages.get(0).getRole());
        Assert.assertEquals("You are a code expert", messages.get(0).getContent().getText());
        
        // 历史对话应该保留
        Assert.assertEquals(5, messages.size());
    }
}
