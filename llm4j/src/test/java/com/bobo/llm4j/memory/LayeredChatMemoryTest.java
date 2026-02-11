package com.bobo.llm4j.memory;

import com.bobo.llm4j.chat.entity.Message;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * LayeredChatMemory 测试
 *
 * @author bobo
 * @since 1.0.0
 */
public class LayeredChatMemoryTest {

    private DailyNotesStore dailyNotesStore;
    private LongTermMemoryStore longTermMemoryStore;
    private MemoryDistillationService distillationService;
    private LayeredChatMemory chatMemory;

    @Before
    public void setUp() {
        dailyNotesStore = new InMemoryDailyNotesStore();
        longTermMemoryStore = new InMemoryLongTermMemoryStore();
        
        distillationService = MemoryDistillationService.builder(dailyNotesStore, longTermMemoryStore)
                .threshold(3) // 3条对话触发提炼
                .build();
        
        chatMemory = LayeredChatMemory.builder()
                .dailyNotesStore(dailyNotesStore)
                .longTermMemoryStore(longTermMemoryStore)
                .distillationService(distillationService)
                .recentDays(2)
                .build();
    }

    @After
    public void tearDown() {
        if (distillationService != null) {
            distillationService.shutdown();
        }
    }

    // ==================== 基础功能测试 ====================

    @Test
    public void testAddMessages_SavesToDailyNotes() {
        String conversationId = "test-conv-1";
        
        chatMemory.add(conversationId, Arrays.asList(
                Message.withUser("Hello"),
                Message.withAssistant("Hi there!")
        ));
        
        LocalDate today = LocalDate.now();
        DailyNote todayNote = dailyNotesStore.findByDate(conversationId, today).orElse(null);
        
        Assert.assertNotNull(todayNote);
        Assert.assertEquals(1, todayNote.size());
        
        DailyEntry entry = todayNote.getEntries().get(0);
        Assert.assertEquals("Hello", entry.getUserMessage());
        Assert.assertEquals("Hi there!", entry.getAssistantMessage());
    }

    @Test
    public void testAddMultipleMessages_AppendsToSameDay() {
        String conversationId = "test-conv-2";
        
        chatMemory.add(conversationId, Arrays.asList(
                Message.withUser("Question 1"),
                Message.withAssistant("Answer 1")
        ));
        
        chatMemory.add(conversationId, Arrays.asList(
                Message.withUser("Question 2"),
                Message.withAssistant("Answer 2")
        ));
        
        LocalDate today = LocalDate.now();
        DailyNote todayNote = dailyNotesStore.findByDate(conversationId, today).orElse(null);
        
        Assert.assertNotNull(todayNote);
        Assert.assertEquals(2, todayNote.size());
    }

    @Test
    public void testBuildMemoryContext_Empty() {
        String conversationId = "test-conv-empty";
        String context = chatMemory.buildMemoryContext(conversationId);
        
        Assert.assertNotNull(context);
        Assert.assertTrue(context.isEmpty() || context.trim().isEmpty());
    }

    @Test
    public void testBuildMemoryContext_WithDailyNotes() {
        String conversationId = "test-conv-3";
        
        chatMemory.add(conversationId, Arrays.asList(
                Message.withUser("Test message"),
                Message.withAssistant("Test response")
        ));
        
        String context = chatMemory.buildMemoryContext(conversationId);
        
        Assert.assertNotNull(context);
        Assert.assertTrue(context.contains("Recent Context"));
        Assert.assertTrue(context.contains("Test message"));
    }

    @Test
    public void testBuildMemoryContext_WithLongTermMemory() {
        String conversationId = "test-conv-4";
        
        // 添加长期记忆
        MemoryEntry entry = MemoryEntry.builder()
                .type(MemoryType.USER_PREFERENCE)
                .content("用户喜欢使用Java编程")
                .addTag("java")
                .build();
        
        longTermMemoryStore.save(conversationId, entry);
        
        String context = chatMemory.buildMemoryContext(conversationId);
        
        Assert.assertNotNull(context);
        Assert.assertTrue(context.contains("Long-Term Memory"));
        Assert.assertTrue(context.contains("用户喜欢使用Java编程"));
    }

    @Test
    public void testClear_RemovesAllMemories() {
        String conversationId = "test-conv-5";
        
        // 添加 Daily Notes
        chatMemory.add(conversationId, Arrays.asList(
                Message.withUser("Test"),
                Message.withAssistant("Response")
        ));
        
        // 添加 Long-Term Memory
        longTermMemoryStore.save(conversationId, MemoryEntry.builder()
                .content("Test memory")
                .build());
        
        // 清空
        chatMemory.clear(conversationId);
        
        // 验证
        Assert.assertTrue(dailyNotesStore.findRecent(conversationId, 7).isEmpty());
        Assert.assertTrue(longTermMemoryStore.findAll(conversationId).isEmpty());
    }

    // ==================== 记忆提炼测试 ====================

    @Test
    public void testMemoryDistillation_ExtractsDecisions() throws InterruptedException {
        String conversationId = "test-distill-1";
        
        // 添加包含决策的对话
        for (int i = 0; i < 3; i++) {
            chatMemory.add(conversationId, Arrays.asList(
                    Message.withUser("我们决定采用Spring框架"),
                    Message.withAssistant("好的，Spring是个不错的选择")
            ));
        }
        
        // 等待异步提炼完成
        Thread.sleep(6000);
        
        // 验证长期记忆中是否有提取的决策
        List<MemoryEntry> decisions = longTermMemoryStore.findByType(conversationId, MemoryType.KEY_DECISION);
        Assert.assertFalse(decisions.isEmpty());
    }

    // ==================== 边界条件测试 ====================

    @Test(expected = IllegalArgumentException.class)
    public void testAdd_NullConversationId() {
        chatMemory.add(null, Collections.singletonList(Message.withUser("Test")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAdd_EmptyConversationId() {
        chatMemory.add("", Collections.singletonList(Message.withUser("Test")));
    }

    @Test
    public void testAdd_EmptyMessages() {
        String conversationId = "test-empty";
        chatMemory.add(conversationId, Collections.<Message>emptyList());
        
        LocalDate today = LocalDate.now();
        DailyNote todayNote = dailyNotesStore.findByDate(conversationId, today).orElse(null);
        
        // 空消息不应该创建 DailyNote
        Assert.assertNull(todayNote);
    }

    @Test
    public void testAdd_OnlySystemMessage() {
        String conversationId = "test-system";
        chatMemory.add(conversationId, Collections.singletonList(Message.withSystem("System prompt")));
        
        LocalDate today = LocalDate.now();
        DailyNote todayNote = dailyNotesStore.findByDate(conversationId, today).orElse(null);
        
        // 只有 SystemMessage 不应该创建 DailyEntry
        Assert.assertNull(todayNote);
    }

    // ==================== 存储接口测试 ====================

    @Test
    public void testDailyNotesStore_FindRecent() {
        String conversationId = "test-recent";
        
        // 添加多条消息
        for (int i = 0; i < 5; i++) {
            chatMemory.add(conversationId, Arrays.asList(
                    Message.withUser("Message " + i),
                    Message.withAssistant("Response " + i)
            ));
        }
        
        List<DailyNote> recentNotes = dailyNotesStore.findRecent(conversationId, 2);
        
        Assert.assertFalse(recentNotes.isEmpty());
        Assert.assertTrue(recentNotes.size() <= 2);
    }

    @Test
    public void testLongTermMemoryStore_FindByType() {
        String conversationId = "test-find-type";
        
        longTermMemoryStore.save(conversationId, MemoryEntry.builder()
                .type(MemoryType.USER_PREFERENCE)
                .content("Preference 1")
                .build());
        
        longTermMemoryStore.save(conversationId, MemoryEntry.builder()
                .type(MemoryType.KEY_DECISION)
                .content("Decision 1")
                .build());
        
        List<MemoryEntry> preferences = longTermMemoryStore.findByType(conversationId, MemoryType.USER_PREFERENCE);
        
        Assert.assertEquals(1, preferences.size());
        Assert.assertEquals("Preference 1", preferences.get(0).getContent());
    }

    @Test
    public void testLongTermMemoryStore_FindByTag() {
        String conversationId = "test-find-tag";
        
        longTermMemoryStore.save(conversationId, MemoryEntry.builder()
                .content("Java related memory")
                .addTag("java")
                .addTag("programming")
                .build());
        
        List<MemoryEntry> javaMemories = longTermMemoryStore.findByTag(conversationId, "java");
        
        Assert.assertEquals(1, javaMemories.size());
        Assert.assertTrue(javaMemories.get(0).getTags().contains("java"));
    }

    // ==================== Builder 测试 ====================

    @Test
    public void testBuilder_DefaultValues() {
        LayeredChatMemory memory = LayeredChatMemory.builder().build();
        
        Assert.assertNotNull(memory);
        Assert.assertNotNull(memory.getDailyNotesStore());
        Assert.assertNotNull(memory.getLongTermMemoryStore());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilder_NullDailyNotesStore() {
        LayeredChatMemory.builder()
                .dailyNotesStore(null)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilder_NullLongTermMemoryStore() {
        LayeredChatMemory.builder()
                .longTermMemoryStore(null)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilder_InvalidRecentDays() {
        LayeredChatMemory.builder()
                .recentDays(0)
                .build();
    }

    // ==================== 实际场景测试 ====================

    @Test
    public void testRealWorldScenario_MultiDayConversation() {
        String conversationId = "real-world-1";
        
        // 第一天的对话
        chatMemory.add(conversationId, Arrays.asList(
                Message.withUser("开始一个新项目"),
                Message.withAssistant("好的，需要什么帮助？")
        ));
        
        // 添加长期记忆
        longTermMemoryStore.save(conversationId, MemoryEntry.builder()
                .type(MemoryType.RECURRING_CONTEXT)
                .content("项目：llm4j - Java LLM客户端库")
                .build());
        
        // 构建上下文
        String context = chatMemory.buildMemoryContext(conversationId);
        
        Assert.assertNotNull(context);
        Assert.assertTrue(context.contains("Long-Term Memory"));
        Assert.assertTrue(context.contains("Recent Context"));
        Assert.assertTrue(context.contains("llm4j"));
    }
}
