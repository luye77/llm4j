package com.bobo.llm4j.chat.client;

import com.bobo.llm4j.annotation.Nullable;
import com.bobo.llm4j.chat.client.advisor.Advisor;
import com.bobo.llm4j.chat.converter.ParameterizedTypeReference;
import com.bobo.llm4j.chat.model.ChatModel;
import com.bobo.llm4j.chat.prompt.ChatOptions;
import com.bobo.llm4j.http.Flux;
import com.bobo.llm4j.http.MimeType;
import com.bobo.llm4j.http.ResponseEntity;
import com.bobo.llm4j.platform.openai.chat.entity.ChatResponse;
import com.bobo.llm4j.platform.openai.chat.entity.Media;
import com.bobo.llm4j.platform.openai.chat.entity.Message;
import com.bobo.llm4j.platform.openai.chat.entity.Prompt;
import com.bobo.llm4j.template.TemplateRenderer;
import com.bobo.llm4j.tool.ToolCallback;
import com.bobo.llm4j.tool.ToolCallbackProvider;

import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Client to perform stateless requests to an AI Model, using a fluent API.
 * <p>
 * Use {@link ChatClient#builder(ChatModel)} to create an instance.
 *
 * <p>Example usage:
 * <pre>{@code
 * ChatClient client = ChatClient.builder(chatModel)
 *     .defaultSystem("You are a helpful assistant")
 *     .build();
 * 
 * String response = client.prompt("Hello")
 *     .call()
 *     .content();
 * }</pre>
 *
 * @author Mark Pollack
 * @author Christian Tzolov
 * @author Josh Long
 * @author Arjen Poutsma
 * @author Thomas Vitale
 * @since 1.0.0
 */
public interface ChatClient {

    /**
     * Create a builder for constructing a ChatClient with the given chat model.
     * 
     * @param chatModel the chat model to use
     * @return a builder for creating a ChatClient
     */
    static Builder builder(ChatModel chatModel) {
        return builder(chatModel, ObservationRegistry.NOOP, null);
    }

    /**
     * Create a builder for constructing a ChatClient with the given chat model
     * and observation registry.
     * 
     * @param chatModel the chat model to use
     * @param observationRegistry the observation registry for monitoring
     * @param observationConvention the observation convention (can be null)
     * @return a builder for creating a ChatClient
     */
    static Builder builder(ChatModel chatModel, ObservationRegistry observationRegistry, 
                          ChatClientObservationConvention observationConvention) {
        return new DefaultChatClientBuilder(chatModel, observationRegistry, observationConvention);
    }

    /**
     * Return a builder to create a new ChatClient whose settings are replicated from this ChatClient.
     * 
     * @return a builder initialized with this client's settings
     */
    Builder mutate();

    ChatClientRequestSpec prompt();

    ChatClientRequestSpec prompt(String content);

    ChatClientRequestSpec prompt(Prompt prompt);

    interface PromptUserSpec {

        PromptUserSpec text(String text);

        PromptUserSpec text(Resource text, Charset charset);

        PromptUserSpec text(Resource text);

        PromptUserSpec param(String k, Object v);

        PromptUserSpec params(Map<String, Object> p);

        PromptUserSpec media(Media... media);

        PromptUserSpec media(MimeType mimeType, URL url);

        PromptUserSpec media(MimeType mimeType, Resource resource);

    }

    /**
     * Specification for a prompt system.
     */
    interface PromptSystemSpec {

        PromptSystemSpec text(String text);

        PromptSystemSpec text(Resource text, Charset charset);

        PromptSystemSpec text(Resource text);

        PromptSystemSpec params(Map<String, Object> p);

        PromptSystemSpec param(String k, Object v);

    }

    interface AdvisorSpec {

        AdvisorSpec param(String k, Object v);

        AdvisorSpec params(Map<String, Object> p);

        AdvisorSpec advisors(Advisor... advisors);

        AdvisorSpec advisors(List<Advisor> advisors);

    }

    interface CallResponseSpec {

        @Nullable
        <T> T entity(Class<T> type);

        ChatClientResponse chatClientResponse();

        @Nullable
        ChatResponse chatResponse();

        @Nullable
        String content();

        <T> ResponseEntity<ChatResponse, T> responseEntity(Class<T> type);

        <T> ResponseEntity<ChatResponse, T> responseEntity(ParameterizedTypeReference<T> type);

        <T> ResponseEntity<ChatResponse, T> responseEntity(StructuredOutputConverter<T> structuredOutputConverter);

        @Nullable
        <T> T entity(ParameterizedTypeReference<T> type);

        @Nullable
        <T> T entity(StructuredOutputConverter<T> structuredOutputConverter);
    }

    interface StreamResponseSpec {

        Flux<ChatClientResponse> chatClientResponse();

        Flux<ChatResponse> chatResponse();

        Flux<String> content();

    }

    interface CallPromptResponseSpec {

        String content();

        List<String> contents();

        ChatResponse chatResponse();

    }

    interface StreamPromptResponseSpec {

        Flux<ChatResponse> chatResponse();

        Flux<String> content();

    }

    interface ChatClientRequestSpec {

        ChatClientRequestSpec advisors(Consumer<AdvisorSpec> consumer);

        ChatClientRequestSpec advisors(Advisor... advisors);

        ChatClientRequestSpec advisors(List<Advisor> advisors);

        ChatClientRequestSpec messages(Message... messages);

        ChatClientRequestSpec messages(List<Message> messages);

        <T extends ChatOptions> ChatClientRequestSpec options(T options);

        ChatClientRequestSpec toolNames(String... toolNames);

        ChatClientRequestSpec tools(Object... toolObjects);

        ChatClientRequestSpec toolCallbacks(ToolCallback... toolCallbacks);

        ChatClientRequestSpec toolCallbacks(List<ToolCallback> toolCallbacks);

        ChatClientRequestSpec toolCallbacks(ToolCallbackProvider... toolCallbackProviders);

        ChatClientRequestSpec toolContext(Map<String, Object> toolContext);

        ChatClientRequestSpec system(String text);

        ChatClientRequestSpec system(Resource textResource, Charset charset);

        ChatClientRequestSpec system(Resource text);

        ChatClientRequestSpec system(Consumer<PromptSystemSpec> consumer);

        ChatClientRequestSpec user(String text);

        ChatClientRequestSpec user(Resource text, Charset charset);

        ChatClientRequestSpec user(Resource text);

        ChatClientRequestSpec user(Consumer<PromptUserSpec> consumer);

        ChatClientRequestSpec templateRenderer(TemplateRenderer templateRenderer);

        CallResponseSpec call();

        StreamResponseSpec stream();

    }

    /**
     * A mutable builder for creating a {@link ChatClient}.
     */
    interface Builder {

        Builder defaultAdvisors(Advisor... advisor);

        Builder defaultAdvisors(Consumer<AdvisorSpec> advisorSpecConsumer);

        Builder defaultAdvisors(List<Advisor> advisors);

        Builder defaultOptions(ChatOptions chatOptions);

        Builder defaultUser(String text);

        Builder defaultUser(Resource text, Charset charset);

        Builder defaultUser(Resource text);

        Builder defaultUser(Consumer<PromptUserSpec> userSpecConsumer);

        Builder defaultSystem(String text);

        Builder defaultSystem(Resource text, Charset charset);

        Builder defaultSystem(Resource text);

        Builder defaultSystem(Consumer<PromptSystemSpec> systemSpecConsumer);

        Builder defaultTemplateRenderer(TemplateRenderer templateRenderer);

        Builder defaultToolNames(String... toolNames);

        Builder defaultTools(Object... toolObjects);

        Builder defaultToolCallbacks(ToolCallback... toolCallbacks);

        Builder defaultToolCallbacks(List<ToolCallback> toolCallbacks);

        Builder defaultToolCallbacks(ToolCallbackProvider... toolCallbackProviders);

        Builder defaultToolContext(Map<String, Object> toolContext);

        Builder clone();

        ChatClient build();

    }

}

