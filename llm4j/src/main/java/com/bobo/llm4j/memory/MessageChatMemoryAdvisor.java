package com.bobo.llm4j.memory;

import com.bobo.llm4j.chat.client.ChatClientRequest;
import com.bobo.llm4j.chat.client.ChatClientResponse;
import com.bobo.llm4j.chat.client.advisor.AdvisorChain;
import com.bobo.llm4j.chat.client.advisor.BaseAdvisor;
import com.bobo.llm4j.chat.entity.ChatResponse;
import com.bobo.llm4j.chat.entity.Generation;
import com.bobo.llm4j.chat.entity.Message;
import com.bobo.llm4j.enums.MessageType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MessageChatMemoryAdvisor - 会话记忆 Advisor（支持双层记忆架构）
 * <p>
 * 参考 Spring AI 的 MessageChatMemoryAdvisor 设计，增强支持 LayeredChatMemory
 * 通过 Advisor 模式自动管理会话记忆，拦截请求和响应
 * 
 * <p>工作流程：
 * <ul>
 *   <li>请求前：从 ChatMemory 获取记忆上下文，注入到 system prompt</li>
 *   <li>请求后：将用户消息和 AI 回复保存到 ChatMemory</li>
 * </ul>
 * <p>
 *
 * @author bobo
 * @since 1.0.0
 */
public final class MessageChatMemoryAdvisor implements BaseAdvisor {

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
     * 请求前处理：获取记忆上下文并注入到请求中
     *
     * @param request 请求对象
     * @param chain Advisor 链
     * @return 处理后的请求
     */
    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
        String conversationId = getConversationId(request.getContext(), this.defaultConversationId);

        List<Message> processedMessages = new ArrayList<>();

        // LayeredChatMemory：构建记忆上下文并注入到 system prompt
        LayeredChatMemory layeredMemory = (LayeredChatMemory) this.chatMemory;
        String memoryContext = layeredMemory.buildMemoryContext(conversationId);

        // 如果有记忆上下文，增强或创建 system message
        if (!memoryContext.trim().isEmpty()) {
            Message enhancedSystemMessage = createOrEnhanceSystemMessage(
                    request.getMessages(),
                    memoryContext
            );
            processedMessages.add(enhancedSystemMessage);
        }

        // 添加当前请求的其他消息（排除原始 system message）
        if (request.getMessages() != null) {
            processedMessages.addAll(filterOutSystemMessages(request.getMessages()));
        }

        // 创建新的请求
        return new ChatClientRequest(
                processedMessages,
                request.getOptions(),
                request.getContext(),
                request.getMedia()
        );
    }

    /**
     * 请求后处理：保存对话到会话记忆
     *
     * @param response 响应对象
     * @param chain Advisor 链
     * @return 原始响应
     */
    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
        String conversationId = getConversationId(response.getMetadata(), this.defaultConversationId);
        
        // 提取用户消息和助手消息

        // 从响应元数据中提取原始用户消息（如果可用）
        // 注意：这里假设在请求前已经保存了用户消息，这里主要保存助手回复
        List<Message> assistantMessages = extractAssistantMessages(response);
        List<Message> messagesToSave = new ArrayList<>(assistantMessages);
        
        if (!messagesToSave.isEmpty()) {
            this.chatMemory.add(conversationId, messagesToSave);
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
     * 创建或增强 SystemMessage（注入记忆上下文）
     */
    private Message createOrEnhanceSystemMessage(List<Message> messages, String memoryContext) {
        Message existingSystemMessage = getFirstSystemMessage(messages);
        
        if (existingSystemMessage != null) {
            // 增强现有的 system message
            String originalContent = existingSystemMessage.getContent() != null 
                ? existingSystemMessage.getContent().getText() 
                : "";
            
            String enhancedContent = originalContent + "\n\n" + memoryContext;
            return Message.withSystem(enhancedContent);
        } else {
            // 创建新的 system message
            return Message.withSystem(memoryContext);
        }
    }
    
    /**
     * 获取第一条 SystemMessage
     */
    private Message getFirstSystemMessage(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        
        for (Message message : messages) {
            if (message != null && MessageType.SYSTEM.getRole().equals(message.getRole())) {
                return message;
            }
        }
        
        return null;
    }
    
    /**
     * 过滤掉 SystemMessage
     */
    private List<Message> filterOutSystemMessages(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<Message> filtered = new ArrayList<>();
        for (Message message : messages) {
            if (message != null && !MessageType.SYSTEM.getRole().equals(message.getRole())) {
                filtered.add(message);
            }
        }
        
        return filtered;
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
