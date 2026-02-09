package com.bobo.llm4j.memory;

import com.bobo.llm4j.chat.client.ChatClientRequest;
import com.bobo.llm4j.chat.client.ChatClientResponse;
import com.bobo.llm4j.chat.client.advisor.Advisor;
import com.bobo.llm4j.chat.client.advisor.BaseAdvisorChain;
import com.bobo.llm4j.chat.entity.ChatResponse;
import com.bobo.llm4j.chat.entity.Generation;
import com.bobo.llm4j.chat.entity.Message;
import com.bobo.llm4j.enums.MessageType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MessageChatMemoryAdvisor - 会话记忆 Advisor
 * <p>
 * 参考 Spring AI 的 MessageChatMemoryAdvisor 设计
 * 通过 Advisor 模式自动管理会话记忆，拦截请求和响应
 * 
 * <p>工作流程：
 * <ul>
 *   <li>请求前：从 ChatMemory 获取历史消息，合并到当前请求中</li>
 *   <li>请求后：将用户消息和 AI 回复保存到 ChatMemory</li>
 * </ul>
 *
 * @author bobo
 * @since 1.0.0
 */
public final class MessageChatMemoryAdvisor implements Advisor {

    private static final String NAME = "MessageChatMemoryAdvisor";
    private static final int DEFAULT_ORDER = 0;

    private final ChatMemory chatMemory;
    private final String defaultConversationId;
    private final int order;

    private MessageChatMemoryAdvisor(ChatMemory chatMemory, String defaultConversationId, int order) {
        if (chatMemory == null) {
            throw new IllegalArgumentException("chatMemory cannot be null");
        }
        if (defaultConversationId == null || defaultConversationId.trim().isEmpty()) {
            throw new IllegalArgumentException("defaultConversationId cannot be null or empty");
        }
        this.chatMemory = chatMemory;
        this.defaultConversationId = defaultConversationId;
        this.order = order;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getOrder() {
        return order;
    }

    /**
     * 请求前处理：获取历史消息并合并到当前请求
     *
     * @param request 请求对象
     * @param chain Advisor 链
     * @return 处理后的请求
     */
    public ChatClientRequest before(ChatClientRequest request, BaseAdvisorChain chain) {
        String conversationId = getConversationId(request.getContext(), this.defaultConversationId);

        // 1. 获取会话历史
        List<Message> memoryMessages = this.chatMemory.get(conversationId);

        // 2. 构建完整消息列表（历史消息 + 当前消息）
        List<Message> processedMessages = new ArrayList<>(memoryMessages);
        
        if (request.getMessages() != null) {
            processedMessages.addAll(request.getMessages());
        }

        // 3. 确保 SystemMessage 在第一位（符合 OpenAI 规范）
        processedMessages = ensureSystemMessageFirst(processedMessages);

        // 4. 保存当前用户消息到记忆
        Message userMessage = getLastUserMessage(request.getMessages());
        if (userMessage != null) {
            this.chatMemory.add(conversationId, userMessage);
        }

        // 5. 创建新的请求
        return new ChatClientRequest(
                processedMessages,
                request.getOptions(),
                request.getContext(),
                request.getMedia()
        );
    }

    /**
     * 请求后处理：保存 AI 回复到会话记忆
     *
     * @param response 响应对象
     * @param chain Advisor 链
     * @return 原始响应
     */
    public ChatClientResponse after(ChatClientResponse response, BaseAdvisorChain chain) {
        List<Message> assistantMessages = extractAssistantMessages(response);
        
        if (!assistantMessages.isEmpty()) {
            String conversationId = getConversationId(response.getMetadata(), this.defaultConversationId);
            this.chatMemory.add(conversationId, assistantMessages);
        }
        
        return response;
    }

    /**
     * 从上下文中获取会话ID
     */
    private String getConversationId(Map<String, Object> context, String defaultConversationId) {
        if (context == null) {
            return defaultConversationId;
        }
        
        Object conversationId = context.get(ChatMemory.CONVERSATION_ID);
        if (conversationId != null) {
            return conversationId.toString();
        }
        
        return defaultConversationId;
    }

    /**
     * 确保 SystemMessage 在消息列表的第一位
     */
    private List<Message> ensureSystemMessageFirst(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return messages;
        }

        List<Message> result = new ArrayList<>(messages);
        
        // 查找 SystemMessage
        for (int i = 0; i < result.size(); i++) {
            Message message = result.get(i);
            if (message != null && MessageType.SYSTEM.getRole().equals(message.getRole())) {
                // 如果不在第一位，移动到第一位
                if (i != 0) {
                    result.remove(i);
                    result.add(0, message);
                }
                break;
            }
        }
        
        return result;
    }

    /**
     * 获取最后一条用户消息
     */
    private Message getLastUserMessage(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return null;
        }

        for (int i = messages.size() - 1; i >= 0; i--) {
            Message message = messages.get(i);
            if (message != null && MessageType.USER.getRole().equals(message.getRole())) {
                return message;
            }
        }
        
        return null;
    }

    /**
     * 提取 AI 回复消息
     */
    private List<Message> extractAssistantMessages(ChatClientResponse response) {
        List<Message> assistantMessages = new ArrayList<>();
        
        if (response == null || response.chatResponse() == null) {
            return assistantMessages;
        }

        ChatResponse chatResponse = response.chatResponse();
        if (chatResponse.getGenerations() != null) {
            for (Generation generation : chatResponse.getGenerations()) {
                if (generation != null && generation.getMessage() != null) {
                    assistantMessages.add(generation.getMessage());
                }
            }
        }
        
        return assistantMessages;
    }

    /**
     * 创建构建器
     */
    public static Builder builder(ChatMemory chatMemory) {
        return new Builder(chatMemory);
    }

    /**
     * 构建器
     */
    public static final class Builder {
        private final ChatMemory chatMemory;
        private String conversationId = ChatMemory.DEFAULT_CONVERSATION_ID;
        private int order = DEFAULT_ORDER;

        private Builder(ChatMemory chatMemory) {
            this.chatMemory = chatMemory;
        }

        /**
         * 设置默认会话ID
         */
        public Builder conversationId(String conversationId) {
            this.conversationId = conversationId;
            return this;
        }

        /**
         * 设置执行顺序
         */
        public Builder order(int order) {
            this.order = order;
            return this;
        }

        /**
         * 构建 Advisor
         */
        public MessageChatMemoryAdvisor build() {
            return new MessageChatMemoryAdvisor(this.chatMemory, this.conversationId, this.order);
        }
    }
}
