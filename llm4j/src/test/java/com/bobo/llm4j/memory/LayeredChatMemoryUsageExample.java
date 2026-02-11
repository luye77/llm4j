package com.bobo.llm4j.memory;

import com.bobo.llm4j.chat.entity.Message;

import java.util.Arrays;
import java.util.List;

/**
 * LayeredChatMemory 使用示例
 *
 * @author bobo
 * @since 1.0.0
 */
public class LayeredChatMemoryUsageExample {

    /**
     * 重复字符串（Java 8兼容）
     */
    private static String repeatString(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
    
    public static void main(String[] args) throws InterruptedException {
        // ==================== 示例1：基本使用 ====================
        System.out.println("=== 示例1：基本使用（内存存储） ===\n");
        basicUsageExample();
        
        System.out.println("\n" + repeatString("=", 60) + "\n");
        
        // ==================== 示例2：启用智能提炼 ====================
        System.out.println("=== 示例2：启用智能提炼 ===\n");
        distillationExample();
        
        System.out.println("\n" + repeatString("=", 60) + "\n");
        
        // ==================== 示例3：手动管理长期记忆 ====================
        System.out.println("=== 示例3：手动管理长期记忆 ===\n");
        manualMemoryExample();
    }

    /**
     * 示例1：基本使用
     */
    private static void basicUsageExample() {
        // 创建双层记忆管理器（默认使用内存存储）
        LayeredChatMemory memory = LayeredChatMemory.builder()
                .recentDays(2)  // 读取最近2天的 Daily Notes
                .build();

        // 模拟对话
        String conversationId = "user-123";
        
        System.out.println("添加第一轮对话...");
        memory.add(conversationId, Arrays.asList(
                Message.withUser("你好，我想学习Java"),
                Message.withAssistant("很好！Java是一门优秀的编程语言。")
        ));

        System.out.println("添加第二轮对话...");
        memory.add(conversationId, Arrays.asList(
                Message.withUser("推荐一些学习资源"),
                Message.withAssistant("我推荐《Effective Java》这本书。")
        ));

        // 获取记忆上下文
        System.out.println("\n构建记忆上下文：");
        String context = memory.buildMemoryContext(conversationId);
        System.out.println(context);
    }

    /**
     * 示例2：启用智能提炼
     */
    private static void distillationExample() throws InterruptedException {
        // 创建存储
        DailyNotesStore dailyNotesStore = new InMemoryDailyNotesStore();
        LongTermMemoryStore longTermMemoryStore = new InMemoryLongTermMemoryStore();

        // 创建提炼服务
        MemoryDistillationService distillationService = MemoryDistillationService
                .builder(dailyNotesStore, longTermMemoryStore)
                .threshold(3)  // 每3条对话触发一次提炼
                .build();

        // 创建带提炼功能的记忆管理器
        LayeredChatMemory memory = LayeredChatMemory.builder()
                .dailyNotesStore(dailyNotesStore)
                .longTermMemoryStore(longTermMemoryStore)
                .distillationService(distillationService)
                .recentDays(2)
                .build();

        String conversationId = "user-456";

        // 添加包含决策的对话
        System.out.println("添加对话1（包含决策）...");
        memory.add(conversationId, Arrays.asList(
                Message.withUser("我决定采用Spring Boot框架"),
                Message.withAssistant("好的，Spring Boot是个不错的选择！")
        ));

        System.out.println("添加对话2...");
        memory.add(conversationId, Arrays.asList(
                Message.withUser("我喜欢用Maven管理依赖"),
                Message.withAssistant("Maven确实很方便。")
        ));

        System.out.println("添加对话3...");
        memory.add(conversationId, Arrays.asList(
                Message.withUser("如何配置数据库？"),
                Message.withAssistant("可以在application.yml中配置。")
        ));

        // 等待异步提炼完成
        System.out.println("\n等待提炼服务处理...");
        Thread.sleep(6000);

        // 查看提炼结果
        System.out.println("\n提炼后的长期记忆：");
        List<MemoryEntry> memories = longTermMemoryStore.findAll(conversationId);
        for (MemoryEntry entry : memories) {
            System.out.println("- [" + entry.getType().getDescription() + "] " + entry.getContent());
        }

        // 关闭提炼服务
        distillationService.shutdown();
    }

    /**
     * 示例3：手动管理长期记忆
     */
    private static void manualMemoryExample() {
        LayeredChatMemory memory = LayeredChatMemory.builder().build();
        String conversationId = "user-789";

        // 手动添加用户偏好
        System.out.println("手动添加用户偏好...");
        memory.getLongTermMemoryStore().save(conversationId,
                MemoryEntry.builder()
                        .type(MemoryType.USER_PREFERENCE)
                        .content("用户是Java后端开发者，5年经验，擅长Spring生态")
                        .addTag("java")
                        .addTag("spring")
                        .addTag("backend")
                        .build()
        );

        // 手动添加项目上下文
        System.out.println("手动添加项目上下文...");
        memory.getLongTermMemoryStore().save(conversationId,
                MemoryEntry.builder()
                        .type(MemoryType.RECURRING_CONTEXT)
                        .content("当前项目：llm4j - Java LLM客户端库，参考Spring AI设计")
                        .addTag("llm4j")
                        .addTag("project")
                        .build()
        );

        // 手动添加关键决策
        System.out.println("手动添加关键决策...");
        memory.getLongTermMemoryStore().save(conversationId,
                MemoryEntry.builder()
                        .type(MemoryType.KEY_DECISION)
                        .content("采用双层记忆架构：Daily Notes + Long-Term Memory")
                        .addTag("architecture")
                        .addTag("memory")
                        .build()
        );

        // 添加一些对话
        memory.add(conversationId, Arrays.asList(
                Message.withUser("今天进展如何？"),
                Message.withAssistant("已完成LayeredChatMemory的实现。")
        ));

        // 查看完整的记忆上下文
        System.out.println("\n完整的记忆上下文：");
        System.out.println(memory.buildMemoryContext(conversationId));

        // 按类型查询
        System.out.println("\n用户偏好列表：");
        List<MemoryEntry> preferences = memory.getLongTermMemoryStore()
                .findByType(conversationId, MemoryType.USER_PREFERENCE);
        for (MemoryEntry entry : preferences) {
            System.out.println("- " + entry.getContent() + " " + entry.getTags());
        }

        // 按标签查询
        System.out.println("\nJava相关记忆：");
        List<MemoryEntry> javaMemories = memory.getLongTermMemoryStore()
                .findByTag(conversationId, "java");
        for (MemoryEntry entry : javaMemories) {
            System.out.println("- " + entry.getContent());
        }

        // 清空记忆
        System.out.println("\n清空所有记忆...");
        memory.clear(conversationId);
        System.out.println("记忆已清空，上下文为空：" + memory.buildMemoryContext(conversationId).isEmpty());
    }
}
