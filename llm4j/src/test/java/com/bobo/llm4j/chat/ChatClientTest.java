package com.bobo.llm4j.chat;

import com.bobo.llm4j.chat.client.ChatClient;
import com.bobo.llm4j.chat.model.ChatModel;
import com.bobo.llm4j.chat.prompt.ChatOptions;
import com.bobo.llm4j.platform.openai.chat.entity.ChatResponse;
import com.bobo.llm4j.platform.openai.chat.entity.Message;
import com.bobo.llm4j.platform.openai.chat.entity.Prompt;
import com.bobo.llm4j.platform.qwen.chat.QwenChatModel;
import com.bobo.llm4j.service.Configuration;

/**
 * Example demonstrating how to use ChatClient to avoid directly calling ChatModel
 *
 * <p>ChatClient provides a fluent API for inte racting with chat models, offering:
 * <ul>
 *   <li>Simplified request building</li>
 *   <li>Default configuration management</li>
 *   <li>Advisor pattern for request/response interception</li>
 *   <li>Template rendering for prompts</li>
 *   <li>Tool/function calling support</li>
 * </ul>
 *
 * @author llm4j
 */
public class ChatClientTest {

    /**
     * Example 1: Basic usage with simple text prompt
     */
    public static void example1_BasicUsage() throws Exception {
        // Create a chat model adapter
        ChatModel chatModel = new QwenChatModelAdapter(new QwenChatModel(new Configuration()));

        // Create ChatClient using builder pattern
        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultSystem("You are a helpful AI assistant.")
                .build();

        // Send a simple prompt
        String response = chatClient.prompt("What is the capital of France?")
                .call()
                .content();

        System.out.println("Response: " + response);
    }

    /**
     * Example 2: Using fluent API with system and user messages
     */
    public static void example2_FluentAPI() throws Exception {
        ChatModel chatModel = new QwenChatModelAdapter(new QwenChatModel(new Configuration()));

        // Create client without default configuration
        ChatClient chatClient = ChatClient.builder(chatModel).build();

        // Use fluent API to build complex requests
        String response = chatClient.prompt()
                .system("You are a technical expert in Java programming.")
                .user("Explain what is a lambda expression in Java")
                .call()
                .content();

        System.out.println("Response: " + response);
    }

    /**
     * Example 3: Using template parameters
     */
    public static void example3_TemplateParameters() throws Exception {
        ChatModel chatModel = new QwenChatModelAdapter(new QwenChatModel(new Configuration()));

        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultSystem("You are an expert on {topic}.")
                .build();

        // Send prompt with parameters
        String response = chatClient.prompt()
                .system((ChatClient.PromptSystemSpec s) -> s.param("topic", "machine learning"))
                .user("What is gradient descent?")
                .call()
                .content();

        System.out.println("Response: " + response);
    }

    /**
     * Example 4: Reusing client with different prompts
     */
    public static void example4_ReuseClient() throws Exception {
        ChatModel chatModel = new QwenChatModelAdapter(new QwenChatModel(new Configuration()));

        // Create a client with default system message
        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultSystem("You are a helpful assistant. Keep answers concise.")
                .build();

        // Send multiple prompts using the same client
        String response1 = chatClient.prompt("What is Java?").call().content();
        String response2 = chatClient.prompt("What is Python?").call().content();
        String response3 = chatClient.prompt("Compare Java and Python").call().content();

        System.out.println("Response 1: " + response1);
        System.out.println("Response 2: " + response2);
        System.out.println("Response 3: " + response3);
    }

    /**
     * Example 5: Getting full ChatResponse
     */
    public static void example5_FullResponse() throws Exception {
        ChatModel chatModel = new QwenChatModelAdapter(new QwenChatModel(new Configuration()));

        ChatClient chatClient = ChatClient.builder(chatModel).build();

        // Get full ChatResponse instead of just content
        ChatResponse response = chatClient.prompt("Hello, how are you?")
                .call()
                .chatResponse();

        if (response != null && !response.getGenerations().isEmpty()) {
            System.out.println("Model: " + response.getModel());
            System.out.println("Content: " + response.getGenerations().get(0).getMessage().getContent());
            System.out.println("Usage: " + response.getUsage());
        }
    }

    /**
     * Example 6: Using ChatClient with custom messages
     */
    public static void example6_CustomMessages() throws Exception {
        ChatModel chatModel = new QwenChatModelAdapter(new QwenChatModel(new Configuration()));

        ChatClient chatClient = ChatClient.builder(chatModel).build();

        // Build conversation history
        String response = chatClient.prompt()
                .messages(
                        Message.withSystem("You are a math tutor."),
                        Message.withUser("What is 2+2?"),
                        Message.withAssistant("2+2 equals 4."),
                        Message.withUser("What about 3+3?")
                )
                .call()
                .content();

        System.out.println("Response: " + response);
    }

    /**
     * Adapter to convert service.ChatModel to demo.ChatModel
     */
    private static class QwenChatModelAdapter implements ChatModel {
        private final com.bobo.llm4j.service.ChatModel serviceModel;

        public QwenChatModelAdapter(com.bobo.llm4j.service.ChatModel serviceModel) {
            this.serviceModel = serviceModel;
        }

        @Override
        public ChatResponse call(Prompt prompt) throws Exception {
            return serviceModel.call(prompt);
        }

        @Override
        public ChatOptions getDefaultOptions() {
            return null; // Can implement default options here if needed
        }
    }

    public static void main(String[] args) {
        System.out.println("=== ChatClient Examples ===\n");

        try {
            System.out.println("--- Example 1: Basic Usage ---");
            example1_BasicUsage();

            System.out.println("\n--- Example 2: Fluent API ---");
            example2_FluentAPI();

            System.out.println("\n--- Example 3: Template Parameters ---");
            example3_TemplateParameters();

            System.out.println("\n--- Example 4: Reuse Client ---");
            example4_ReuseClient();

            System.out.println("\n--- Example 5: Full Response ---");
            example5_FullResponse();

            System.out.println("\n--- Example 6: Custom Messages ---");
            example6_CustomMessages();

        } catch (Exception e) {
            System.out.println(e.getMessage());
            ;
        }
    }
}

