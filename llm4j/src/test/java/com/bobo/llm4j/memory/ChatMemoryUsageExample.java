package com.bobo.llm4j.memory;

import com.bobo.llm4j.chat.client.ChatClient;
import com.bobo.llm4j.chat.model.ChatModel;
import com.bobo.llm4j.chat.entity.Message;
import com.bobo.llm4j.platform.openai.chat.OpenAiChatModel;
import com.bobo.llm4j.config.Configuration;

import java.util.List;

/**
 * 会话记忆使用示例
 * <p>
 * 演示如何使用重构后的会话记忆功能
 * 
 * @author bobo
 * @since 1.0.0
 */
public class ChatMemoryUsageExample {

    public static void main(String[] args) {
        // ==================== 示例 1: 基础使用 ====================
        basicUsage();

        // ==================== 示例 2: 窗口限制 ====================
        windowLimitDemo();

        // ==================== 示例 3: SystemMessage 处理 ====================
        systemMessageDemo();

        // ==================== 示例 4: 多会话管理 ====================
        multiConversationDemo();

        // ==================== 示例 5: 自定义配置 ====================
        customConfigDemo();
    }

    /**
     * 示例 1: 基础使用
     */
    private static void basicUsage() {
        System.out.println("========== 示例 1: 基础使用 ==========");

        // 创建 ChatMemory
        ChatMemory chatMemory = new MessageWindowChatMemory();
        String conversationId = "user-001";

        // 第一轮对话
        chatMemory.add(conversationId, Message.withUser("你好，我叫张三"));
        chatMemory.add(conversationId, Message.withAssistant("你好张三！很高兴认识你。"));

        // 第二轮对话
        chatMemory.add(conversationId, Message.withUser("我叫什么名字？"));
        chatMemory.add(conversationId, Message.withAssistant("您叫张三。"));

        // 获取会话历史
        List<Message> history = chatMemory.get(conversationId);
        System.out.println("会话消息数: " + history.size());
        for (Message msg : history) {
            System.out.println(msg.getRole() + ": " + msg.getContent().getText());
        }

        System.out.println();
    }

    /**
     * 示例 2: 窗口限制演示
     */
    private static void windowLimitDemo() {
        System.out.println("========== 示例 2: 窗口限制 ==========");

        // 创建最多保留 5 条消息的 ChatMemory
        ChatMemory chatMemory = new MessageWindowChatMemory(5);
        String conversationId = "limited-conv";

        // 添加 7 条消息
        chatMemory.add(conversationId, Message.withUser("消息1"));
        chatMemory.add(conversationId, Message.withUser("消息2"));
        chatMemory.add(conversationId, Message.withUser("消息3"));
        chatMemory.add(conversationId, Message.withUser("消息4"));
        chatMemory.add(conversationId, Message.withUser("消息5"));
        chatMemory.add(conversationId, Message.withUser("消息6"));
        chatMemory.add(conversationId, Message.withUser("消息7"));

        List<Message> history = chatMemory.get(conversationId);
        System.out.println("实际保存的消息数: " + history.size() + " (预期: 5)");
        System.out.println("最旧的消息: " + history.get(0).getContent().getText() + " (应该是消息3)");
        System.out.println("最新的消息: " + history.get(history.size() - 1).getContent().getText() + " (应该是消息7)");

        System.out.println();
    }

    /**
     * 示例 3: SystemMessage 特殊处理
     */
    private static void systemMessageDemo() {
        System.out.println("========== 示例 3: SystemMessage 处理 ==========");

        ChatMemory chatMemory = new MessageWindowChatMemory(10);
        String conversationId = "system-demo";

        // 添加初始 SystemMessage
        chatMemory.add(conversationId, Message.withSystem("你是一个通用助手"));
        chatMemory.add(conversationId, Message.withUser("你好"));
        chatMemory.add(conversationId, Message.withAssistant("你好！我是通用助手"));

        System.out.println("第一轮对话后的消息数: " + chatMemory.get(conversationId).size());
        System.out.println("系统提示: " + chatMemory.get(conversationId).get(0).getContent().getText());

        // 更新 SystemMessage（例如：用户切换了角色）
        chatMemory.add(conversationId, Message.withSystem("你是一个Java编程专家"));
        chatMemory.add(conversationId, Message.withUser("写一个Hello World"));

        List<Message> history = chatMemory.get(conversationId);
        System.out.println("\n更新系统提示后的消息数: " + history.size());
        System.out.println("新的系统提示: " + history.get(0).getContent().getText() + " (旧的已被替换)");

        // 验证只有一个 SystemMessage
        long systemCount = history.stream()
                .filter(m -> "system".equals(m.getRole()))
                .count();
        System.out.println("SystemMessage 数量: " + systemCount + " (应该是1)");

        System.out.println();
    }

    /**
     * 示例 4: 多会话管理
     */
    private static void multiConversationDemo() {
        System.out.println("========== 示例 4: 多会话管理 ==========");

        ChatMemory chatMemory = new MessageWindowChatMemory();

        // 用户 A 的会话
        String convA = "user-A";
        chatMemory.add(convA, Message.withUser("我是用户A"));
        chatMemory.add(convA, Message.withAssistant("你好，用户A"));

        // 用户 B 的会话
        String convB = "user-B";
        chatMemory.add(convB, Message.withUser("我是用户B"));
        chatMemory.add(convB, Message.withAssistant("你好，用户B"));

        // 验证会话隔离
        System.out.println("用户A的消息数: " + chatMemory.get(convA).size());
        System.out.println("用户B的消息数: " + chatMemory.get(convB).size());

        System.out.println("用户A的第一条消息: " + chatMemory.get(convA).get(0).getContent().getText());
        System.out.println("用户B的第一条消息: " + chatMemory.get(convB).get(0).getContent().getText());

        // 清空用户A的会话
        chatMemory.clear(convA);
        System.out.println("\n清空后，用户A的消息数: " + chatMemory.get(convA).size());
        System.out.println("用户B的消息数不受影响: " + chatMemory.get(convB).size());

        System.out.println();
    }

    /**
     * 示例 5: 使用 Builder 自定义配置
     */
    private static void customConfigDemo() {
        System.out.println("========== 示例 5: 自定义配置 ==========");

        // 使用 Builder 创建自定义配置
        ChatMemoryRepository customRepository = new InMemoryChatMemoryRepository();
        
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(customRepository)
                .maxMessages(3)
                .build();

        String conversationId = "custom-config";

        // 添加消息
        chatMemory.add(conversationId, Message.withUser("消息1"));
        chatMemory.add(conversationId, Message.withUser("消息2"));
        chatMemory.add(conversationId, Message.withUser("消息3"));
        chatMemory.add(conversationId, Message.withUser("消息4"));

        System.out.println("自定义窗口大小为3，添加4条消息后实际保存: " + 
                chatMemory.get(conversationId).size());

        // 验证 Repository 也包含相同数据
        System.out.println("Repository 中的消息数: " + 
                customRepository.findByConversationId(conversationId).size());

        System.out.println();
    }

    /**
     * 示例 6: MessageChatMemoryAdvisor 使用（伪代码）
     */
    @SuppressWarnings("unused")
    private static void advisorUsageDemo() {
        System.out.println("========== 示例 6: Advisor 使用 (伪代码) ==========");

        // 创建 ChatMemory
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .maxMessages(10)
                .build();

        // 创建 Advisor
        MessageChatMemoryAdvisor advisor = MessageChatMemoryAdvisor.builder(chatMemory)
                .conversationId("user-123")
                .order(0)
                .build();

        System.out.println("Advisor 名称: " + advisor.getName());
        System.out.println("Advisor 顺序: " + advisor.getOrder());

        Configuration configuration = Configuration.builder()
                .build();

        ChatModel chatModel = new OpenAiChatModel(configuration);

//      在实际使用中，Advisor 会被添加到 ChatClient：
        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(advisor)
                .build();

          String response = chatClient.prompt()
              .user("我叫张三")
              .call()
              .content();

          // 后续请求会自动包含历史上下文
          String response2 = chatClient.prompt()
              .user("我叫什么名字？")
              .call()
              .content();
          // 响应：您叫张三

        System.out.println("\nAdvisor 会自动管理会话记忆，无需手动处理历史消息！");
        System.out.println();
    }
}
