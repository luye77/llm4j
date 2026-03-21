package com.bobo.llm4j.chat.client;

import cn.hutool.core.lang.Assert;
import com.bobo.llm4j.annotation.Nullable;
import com.bobo.llm4j.chat.client.advisor.Advisor;
import com.bobo.llm4j.chat.client.advisor.BaseAdvisorChain;
import com.bobo.llm4j.chat.client.advisor.DefaultAroundAdvisorChain;
import com.bobo.llm4j.chat.client.observe.*;
import com.bobo.llm4j.chat.converter.BeanOutputConverter;
import com.bobo.llm4j.chat.converter.ParameterizedTypeReference;
import com.bobo.llm4j.chat.model.ChatModel;
import com.bobo.llm4j.chat.model.ChatModelCallAdvisor;
import com.bobo.llm4j.chat.model.ChatModelStreamAdvisor;
import com.bobo.llm4j.chat.util.DefaultChatClientUtils;
import com.bobo.llm4j.chat.util.StringUtils;
import com.bobo.llm4j.http.Flux;
import com.bobo.llm4j.http.MimeType;
import com.bobo.llm4j.http.ResponseEntity;
import com.bobo.llm4j.chat.entity.ChatResponse;
import com.bobo.llm4j.chat.entity.Generation;
import com.bobo.llm4j.chat.entity.Media;
import com.bobo.llm4j.chat.entity.Message;
import com.bobo.llm4j.chat.entity.Prompt;
import com.bobo.llm4j.template.StTemplateRenderer;
import com.bobo.llm4j.template.TemplateRenderer;
import lombok.Getter;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.Consumer;

public class DefaultChatClient implements ChatClient {

    private static final ChatClientObservationConvention DEFAULT_CHAT_CLIENT_OBSERVATION_CONVENTION = new DefaultChatClientObservationConvention();

    private static final TemplateRenderer DEFAULT_TEMPLATE_RENDERER = StTemplateRenderer.builder().build();

    private final DefaultChatClientRequestSpec defaultChatClientRequest;

    public DefaultChatClient(DefaultChatClientRequestSpec defaultChatClientRequest) {
        Assert.notNull(defaultChatClientRequest, "defaultChatClientRequest cannot be null");
        this.defaultChatClientRequest = defaultChatClientRequest;
    }

    @Override
    public ChatClientRequestSpec prompt() {
        return new DefaultChatClientRequestSpec(this.defaultChatClientRequest);
    }

    @Override
    public ChatClientRequestSpec prompt(String content) {
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("content cannot be null or empty");
        }
        
        // Build a simple prompt with user message
        DefaultChatClientRequestSpec spec = new DefaultChatClientRequestSpec(this.defaultChatClientRequest);
        spec.user(content);
        return spec;
    }

    @Override
    public ChatClientRequestSpec prompt(Prompt prompt) {
        Assert.notNull(prompt, "prompt cannot be null");

        DefaultChatClientRequestSpec spec = new DefaultChatClientRequestSpec(this.defaultChatClientRequest);

        // Messages
        if (!prompt.getMessages().isEmpty()) {
            spec.messages(prompt.getMessages());
        }

        return spec;
    }

    /**
     * Return a {@code ChatClientBuilder} to create a new {@code ChatClient} whose
     * settings are replicated from this {@code ChatClientRequest}.
     */
    @Override
    public ChatClient.Builder mutate() {
        return this.defaultChatClientRequest.mutate();
    }

    public static class DefaultPromptUserSpec implements PromptUserSpec {

        private final Map<String, Object> params = new HashMap<>();

        private final List<Media> media = new ArrayList<>();

        @Nullable
        private String text;

        @Override
        public PromptUserSpec media(Media... media) {
            Assert.notNull(media, "media cannot be null");
            Assert.noNullElements(media, "media cannot contain null elements");
            this.media.addAll(Arrays.asList(media));
            return this;
        }

        @Override
        public PromptUserSpec media(MimeType mimeType, URL url) {
            Assert.notNull(mimeType, "mimeType cannot be null");
            Assert.notNull(url, "url cannot be null");
            // Media builder not implemented yet, skip for now
            return this;
        }

        @Override
        public PromptUserSpec media(MimeType mimeType, Resource resource) {
            Assert.notNull(mimeType, "mimeType cannot be null");
            Assert.notNull(resource, "resource cannot be null");
            // Media builder not implemented yet, skip for now
            return this;
        }

        @Override
        public PromptUserSpec text(String text) {
            if (!StringUtils.hasText(text)) {
                throw new IllegalArgumentException("text cannot be null or empty");
            }
            this.text = text;
            return this;
        }

        @Override
        public PromptUserSpec text(Resource text, Charset charset) {
            Assert.notNull(text, "text cannot be null");
            Assert.notNull(charset, "charset cannot be null");
            try {
                this.text(text.getContentAsString(charset));
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
            return this;
        }

        @Override
        public PromptUserSpec text(Resource text) {
            Assert.notNull(text, "text cannot be null");
            this.text(text, Charset.defaultCharset());
            return this;
        }

        @Override
        public PromptUserSpec param(String key, Object value) {
            if (!StringUtils.hasText(key)) {
                throw new IllegalArgumentException("key cannot be null or empty");
            }
            Assert.notNull(value, "value cannot be null");
            this.params.put(key, value);
            return this;
        }

        @Override
        public PromptUserSpec params(Map<String, Object> params) {
            Assert.notNull(params, "params cannot be null");
            this.params.putAll(params);
            return this;
        }

        @Nullable
        protected String text() {
            return this.text;
        }

        protected Map<String, Object> params() {
            return this.params;
        }

        protected List<Media> media() {
            return this.media;
        }

    }

    public static class DefaultPromptSystemSpec implements PromptSystemSpec {

        private final Map<String, Object> params = new HashMap<>();

        @Nullable
        private String text;

        @Override
        public PromptSystemSpec text(String text) {
            if (!StringUtils.hasText(text)) {
                throw new IllegalArgumentException("text cannot be null or empty");
            }
            this.text = text;
            return this;
        }

        @Override
        public PromptSystemSpec text(Resource text, Charset charset) {
            Assert.notNull(text, "text cannot be null");
            Assert.notNull(charset, "charset cannot be null");
            try {
                this.text(text.getContentAsString(charset));
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
            return this;
        }

        @Override
        public PromptSystemSpec text(Resource text) {
            Assert.notNull(text, "text cannot be null");
            this.text(text, Charset.defaultCharset());
            return this;
        }

        @Override
        public PromptSystemSpec param(String key, Object value) {
            if (!StringUtils.hasText(key)) {
                throw new IllegalArgumentException("key cannot be null or empty");
            }
            Assert.notNull(value, "value cannot be null");
            this.params.put(key, value);
            return this;
        }

        @Override
        public PromptSystemSpec params(Map<String, Object> params) {
            Assert.notNull(params, "params cannot be null");
            this.params.putAll(params);
            return this;
        }

        @Nullable
        protected String text() {
            return this.text;
        }

        protected Map<String, Object> params() {
            return this.params;
        }

    }

    @Getter
    public static class DefaultAdvisorSpec implements AdvisorSpec {

        private final List<Advisor> advisors = new ArrayList<>();

        private final Map<String, Object> params = new HashMap<>();

        @Override
        public AdvisorSpec param(String key, Object value) {
            if (!StringUtils.hasText(key)) {
                throw new IllegalArgumentException("key cannot be null or empty");
            }
            Assert.notNull(value, "value cannot be null");
            this.params.put(key, value);
            return this;
        }

        @Override
        public AdvisorSpec params(Map<String, Object> params) {
            Assert.notNull(params, "params cannot be null");
            this.params.putAll(params);
            return this;
        }

        @Override
        public AdvisorSpec advisors(Advisor... advisors) {
            Assert.notNull(advisors, "advisors cannot be null");
            this.advisors.addAll(Arrays.asList(advisors));
            return this;
        }

        @Override
        public AdvisorSpec advisors(List<Advisor> advisors) {
            Assert.notNull(advisors, "advisors cannot be null");
            this.advisors.addAll(advisors);
            return this;
        }

    }

    public static class DefaultCallResponseSpec implements CallResponseSpec {

        private final ChatClientRequest request;

        private final BaseAdvisorChain advisorChain;

        private final ObservationRegistry observationRegistry;

        private final ChatClientObservationConvention observationConvention;

        public DefaultCallResponseSpec(ChatClientRequest chatClientRequest, BaseAdvisorChain advisorChain,
                                       ObservationRegistry observationRegistry, ChatClientObservationConvention observationConvention) {
            Assert.notNull(chatClientRequest, "chatClientRequest cannot be null");
            Assert.notNull(advisorChain, "advisorChain cannot be null");
            Assert.notNull(observationRegistry, "observationRegistry cannot be null");
            Assert.notNull(observationConvention, "observationConvention cannot be null");

            this.request = chatClientRequest;
            this.advisorChain = advisorChain;
            this.observationRegistry = observationRegistry;
            this.observationConvention = observationConvention;
        }

        @Override
        public <T> ResponseEntity<ChatResponse, T> responseEntity(Class<T> type) {
            Assert.notNull(type, "type cannot be null");
            return doResponseEntity(new BeanOutputConverter<>(type));
        }

        @Override
        public <T> ResponseEntity<ChatResponse, T> responseEntity(ParameterizedTypeReference<T> type) {
            Assert.notNull(type, "type cannot be null");
            return doResponseEntity(new BeanOutputConverter<>(type));
        }

        @Override
        public <T> ResponseEntity<ChatResponse, T> responseEntity(
                StructuredOutputConverter<T> structuredOutputConverter) {
            Assert.notNull(structuredOutputConverter, "structuredOutputConverter cannot be null");
            return doResponseEntity(structuredOutputConverter);
        }

        protected <T> ResponseEntity<ChatResponse, T> doResponseEntity(StructuredOutputConverter<T> outputConverter) {
            Assert.notNull(outputConverter, "structuredOutputConverter cannot be null");
            ChatClientResponse chatClientResponse = doGetObservableChatClientResponse(this.request, outputConverter.getFormat());
            ChatResponse chatResponse = chatClientResponse.chatResponse();
            String responseContent = getContentFromChatResponse(chatResponse);
            if (responseContent == null) {
                return new ResponseEntity<>(chatResponse, null);
            }
            T entity = outputConverter.convert(responseContent);
            return new ResponseEntity<>(chatResponse, entity);
        }

        @Override
        @Nullable
        public <T> T entity(ParameterizedTypeReference<T> type) {
            Assert.notNull(type, "type cannot be null");
            return doSingleWithBeanOutputConverter(new BeanOutputConverter<>(type));
        }

        @Override
        @Nullable
        public <T> T entity(StructuredOutputConverter<T> structuredOutputConverter) {
            Assert.notNull(structuredOutputConverter, "structuredOutputConverter cannot be null");
            return doSingleWithBeanOutputConverter(structuredOutputConverter);
        }

        @Override
        @Nullable
        public <T> T entity(Class<T> type) {
            Assert.notNull(type, "type cannot be null");
            BeanOutputConverter<T> outputConverter = new BeanOutputConverter<>(type);
            return doSingleWithBeanOutputConverter(outputConverter);
        }

        @Nullable
        private <T> T doSingleWithBeanOutputConverter(StructuredOutputConverter<T> outputConverter) {
            ChatResponse chatResponse = doGetObservableChatClientResponse(this.request, outputConverter.getFormat())
                    .chatResponse();
            String stringResponse = getContentFromChatResponse(chatResponse);
            if (stringResponse == null) {
                return null;
            }
            return outputConverter.convert(stringResponse);
        }

        @Override
        public ChatClientResponse chatClientResponse() {
            return doGetObservableChatClientResponse(this.request);
        }

        @Override
        @Nullable
        public ChatResponse chatResponse() {
            return doGetObservableChatClientResponse(this.request).chatResponse();
        }

        @Override
        @Nullable
        public String content() {
            ChatResponse chatResponse = doGetObservableChatClientResponse(this.request).chatResponse();
            return getContentFromChatResponse(chatResponse);
        }

        private ChatClientResponse doGetObservableChatClientResponse(ChatClientRequest chatClientRequest) {
            return doGetObservableChatClientResponse(chatClientRequest, null);
        }

        private ChatClientResponse doGetObservableChatClientResponse(ChatClientRequest chatClientRequest,
                                                                     @Nullable String outputFormat) {

            if (outputFormat != null) {
                chatClientRequest.context().put(ChatClientAttributes.OUTPUT_FORMAT.getKey(), outputFormat);
            }

            ChatClientObservationContext observationContext = ChatClientObservationContext.builder()
                    .request(chatClientRequest)
                    .advisors(this.advisorChain.getCallAdvisors())
                    .stream(false)
                    .format(outputFormat)
                    .build();

            Observation observation = ChatClientObservationDocumentation.AI_CHAT_CLIENT.observation(this.observationConvention,
                    DEFAULT_CHAT_CLIENT_OBSERVATION_CONVENTION, () -> observationContext, this.observationRegistry);

            // CHECKSTYLE:OFF
            ChatClientResponse chatClientResponse = observation.observe(() -> {
                // Apply the advisor chain that terminates with the ChatModelCallAdvisor.
                return this.advisorChain.nextCall(chatClientRequest);
            });
            // CHECKSTYLE:ON
            return chatClientResponse != null ? chatClientResponse : ChatClientResponse.builder().build();
        }

        @Nullable
        private static String getContentFromChatResponse(@Nullable ChatResponse chatResponse) {
            if (chatResponse == null || chatResponse.getGenerations() == null || chatResponse.getGenerations().isEmpty()) {
                return null;
            }
            Generation generation = chatResponse.getGenerations().get(0);
            if (generation.getMessage() == null || generation.getMessage().getContent() == null) {
                return null;
            }
            return generation.getMessage().getContent().getText();
        }

    }

    public static class DefaultStreamResponseSpec implements StreamResponseSpec {

        private final ChatClientRequest request;

        private final BaseAdvisorChain advisorChain;

        private final ObservationRegistry observationRegistry;

        private final ChatClientObservationConvention observationConvention;

        public DefaultStreamResponseSpec(ChatClientRequest chatClientRequest, BaseAdvisorChain advisorChain,
                                         ObservationRegistry observationRegistry, ChatClientObservationConvention observationConvention) {
            Assert.notNull(chatClientRequest, "chatClientRequest cannot be null");
            Assert.notNull(advisorChain, "advisorChain cannot be null");
            Assert.notNull(observationRegistry, "observationRegistry cannot be null");
            Assert.notNull(observationConvention, "observationConvention cannot be null");

            this.request = chatClientRequest;
            this.advisorChain = advisorChain;
            this.observationRegistry = observationRegistry;
            this.observationConvention = observationConvention;
        }

        private Flux<ChatClientResponse> doGetObservableFluxChatResponse(ChatClientRequest chatClientRequest) {
            return Flux.deferContextual(contextView -> {

                ChatClientObservationContext observationContext = ChatClientObservationContext.builder()
                        .request(chatClientRequest)
                        .advisors(this.advisorChain.getStreamAdvisors())
                        .stream(true)
                        .build();

                Observation observation = ChatClientObservationDocumentation.AI_CHAT_CLIENT.observation(
                        this.observationConvention, DEFAULT_CHAT_CLIENT_OBSERVATION_CONVENTION,
                        () -> observationContext, this.observationRegistry);

                observation.parentObservation(null)
                        .start();

                // @formatter:off
                // Apply the advisor chain that terminates with the ChatModelStreamAdvisor.
                return this.advisorChain.nextStream(chatClientRequest)
                        .doOnError(observation::error)
                        .doFinally(s -> observation.stop());
                // @formatter:on
            });
        }

        @Override
        public Flux<ChatClientResponse> chatClientResponse() {
            return doGetObservableFluxChatResponse(this.request);
        }

        @Override
        public Flux<ChatResponse> chatResponse() {
            return doGetObservableFluxChatResponse(this.request).mapNotNull(ChatClientResponse::chatResponse);
        }

        @Override
        public Flux<String> content() {
            // @formatter:off
            return doGetObservableFluxChatResponse(this.request)
                    .mapNotNull(ChatClientResponse::chatResponse)
                    .map(r -> {
                        if (r.getGenerations() == null || r.getGenerations().isEmpty()) {
                            return "";
                        }
                        Generation gen = r.getGenerations().get(0);
                        if (gen.getMessage() == null || gen.getMessage().getContent() == null) {
                            return "";
                        }
                        return gen.getMessage().getContent().getText();
                    })
                    .filter(StringUtils::hasLength);
            // @formatter:on
        }

    }

    @Getter
    public static class DefaultChatClientRequestSpec implements ChatClientRequestSpec {

        private final ObservationRegistry observationRegistry;

        private final ChatClientObservationConvention observationConvention;

        private final ChatModel chatModel;

        private final List<Media> media = new ArrayList<>();

        private final List<Message> messages = new ArrayList<>();

        private final Map<String, Object> userParams = new HashMap<>();

        private final Map<String, Object> systemParams = new HashMap<>();

        private final List<Advisor> advisors = new ArrayList<>();

        private final Map<String, Object> advisorParams = new HashMap<>();

        private TemplateRenderer templateRenderer;

        @Nullable
        private String userText;

        @Nullable
        private String systemText;

        @Nullable
        private ChatOptions chatOptions;

        /* copy constructor */
        DefaultChatClientRequestSpec(DefaultChatClientRequestSpec ccr) {
            this(ccr.chatModel, ccr.userText, ccr.userParams, ccr.systemText, ccr.systemParams,
                    ccr.messages, ccr.media, ccr.chatOptions, ccr.advisors, ccr.advisorParams,
                    ccr.observationRegistry, ccr.observationConvention, ccr.templateRenderer);
        }

        public DefaultChatClientRequestSpec(ChatModel chatModel, @Nullable String userText,
                                            Map<String, Object> userParams, @Nullable String systemText, Map<String, Object> systemParams,
                                            List<Message> messages, List<Media> media,
                                            @Nullable ChatOptions chatOptions, List<Advisor> advisors, Map<String, Object> advisorParams,
                                            ObservationRegistry observationRegistry,
                                            @Nullable ChatClientObservationConvention observationConvention,
                                            @Nullable TemplateRenderer templateRenderer) {

            Assert.notNull(chatModel, "chatModel cannot be null");
            Assert.notNull(userParams, "userParams cannot be null");
            Assert.notNull(systemParams, "systemParams cannot be null");
            Assert.notNull(messages, "messages cannot be null");
            Assert.notNull(media, "media cannot be null");
            Assert.notNull(advisors, "advisors cannot be null");
            Assert.notNull(advisorParams, "advisorParams cannot be null");
            Assert.notNull(observationRegistry, "observationRegistry cannot be null");

            this.chatModel = chatModel;
            this.chatOptions = chatOptions != null ? chatOptions.copy()
                    : (chatModel.getDefaultOptions() != null) ? chatModel.getDefaultOptions().copy() : null;

            this.userText = userText;
            this.userParams.putAll(userParams);
            this.systemText = systemText;
            this.systemParams.putAll(systemParams);

            this.messages.addAll(messages);
            this.media.addAll(media);
            this.advisors.addAll(advisors);
            this.advisorParams.putAll(advisorParams);
            this.observationRegistry = observationRegistry;
            this.observationConvention = observationConvention != null ? observationConvention
                    : DEFAULT_CHAT_CLIENT_OBSERVATION_CONVENTION;
            this.templateRenderer = templateRenderer != null ? templateRenderer : DEFAULT_TEMPLATE_RENDERER;
        }

        @Nullable
        public ChatOptions getChatOptions() {
            return this.chatOptions;
        }

        /**
         * Return a {@code ChatClientBuilder} to create a new {@code ChatClient} whose
         * settings are replicated from this {@code ChatClientRequest}.
         */
        public ChatClient.Builder mutate() {
            DefaultChatClientBuilder builder = (DefaultChatClientBuilder) ChatClient
                    .builder(this.chatModel, this.observationRegistry, this.observationConvention)
                    .defaultTemplateRenderer(this.templateRenderer);

            if (StringUtils.hasText(this.userText)) {
                builder.defaultUser(this.userText);
            }

            if (StringUtils.hasText(this.systemText)) {
                builder.defaultSystem(this.systemText);
            }

            if (this.chatOptions != null) {
                builder.defaultOptions(this.chatOptions);
            }

            builder.addMessages(this.messages);

            return builder;
        }

        public ChatClientRequestSpec advisors(Consumer<AdvisorSpec> consumer) {
            Assert.notNull(consumer, "consumer cannot be null");
            DefaultAdvisorSpec advisorSpec = new DefaultAdvisorSpec();
            consumer.accept(advisorSpec);
            this.advisorParams.putAll(advisorSpec.getParams());
            this.advisors.addAll(advisorSpec.getAdvisors());
            return this;
        }

        public ChatClientRequestSpec advisors(Advisor... advisors) {
            Assert.notNull(advisors, "advisors cannot be null");
            this.advisors.addAll(Arrays.asList(advisors));
            return this;
        }

        public ChatClientRequestSpec advisors(List<Advisor> advisors) {
            Assert.notNull(advisors, "advisors cannot be null");
            this.advisors.addAll(advisors);
            return this;
        }

        public ChatClientRequestSpec messages(Message... messages) {
            Assert.notNull(messages, "messages cannot be null");
            this.messages.addAll(Arrays.asList(messages));
            return this;
        }

        public ChatClientRequestSpec messages(List<Message> messages) {
            Assert.notNull(messages, "messages cannot be null");
            this.messages.addAll(messages);
            return this;
        }

        public <T extends ChatOptions> ChatClientRequestSpec options(T options) {
            Assert.notNull(options, "options cannot be null");
            this.chatOptions = options;
            return this;
        }

        public ChatClientRequestSpec system(String text) {
            if (!StringUtils.hasText(text)) {
                throw new IllegalArgumentException("text cannot be null or empty");
            }
            this.systemText = text;
            return this;
        }

        public ChatClientRequestSpec system(Resource text, Charset charset) {
            Assert.notNull(text, "text cannot be null");
            Assert.notNull(charset, "charset cannot be null");

            try {
                this.systemText = text.getContentAsString(charset);
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
            return this;
        }

        public ChatClientRequestSpec system(Resource text) {
            Assert.notNull(text, "text cannot be null");
            return this.system(text, Charset.defaultCharset());
        }

        public ChatClientRequestSpec system(Consumer<PromptSystemSpec> consumer) {
            Assert.notNull(consumer, "consumer cannot be null");

            DefaultPromptSystemSpec systemSpec = new DefaultPromptSystemSpec();
            consumer.accept(systemSpec);
            this.systemText = StringUtils.hasText(systemSpec.text()) ? systemSpec.text() : this.systemText;
            this.systemParams.putAll(systemSpec.params());

            return this;
        }

        public ChatClientRequestSpec user(String text) {
            if (!StringUtils.hasText(text)) {
                throw new IllegalArgumentException("text cannot be null or empty");
            }
            this.userText = text;
            return this;
        }

        public ChatClientRequestSpec user(Resource text, Charset charset) {
            Assert.notNull(text, "text cannot be null");
            Assert.notNull(charset, "charset cannot be null");

            try {
                this.userText = text.getContentAsString(charset);
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
            return this;
        }

        public ChatClientRequestSpec user(Resource text) {
            Assert.notNull(text, "text cannot be null");
            return this.user(text, Charset.defaultCharset());
        }

        public ChatClientRequestSpec user(Consumer<PromptUserSpec> consumer) {
            Assert.notNull(consumer, "consumer cannot be null");

            DefaultPromptUserSpec us = new DefaultPromptUserSpec();
            consumer.accept(us);
            this.userText = StringUtils.hasText(us.text()) ? us.text() : this.userText;
            this.userParams.putAll(us.params());
            this.media.addAll(us.media());
            return this;
        }

        public ChatClientRequestSpec templateRenderer(TemplateRenderer templateRenderer) {
            Assert.notNull(templateRenderer, "templateRenderer cannot be null");
            this.templateRenderer = templateRenderer;
            return this;
        }

        public CallResponseSpec call() {
            BaseAdvisorChain advisorChain = buildAdvisorChain();
            return new DefaultCallResponseSpec(DefaultChatClientUtils.toChatClientRequest(this), advisorChain,
                    this.observationRegistry, this.observationConvention);
        }

        public StreamResponseSpec stream() {
            BaseAdvisorChain advisorChain = buildAdvisorChain();
            return new DefaultStreamResponseSpec(DefaultChatClientUtils.toChatClientRequest(this), advisorChain,
                    this.observationRegistry, this.observationConvention);
        }

        private BaseAdvisorChain buildAdvisorChain() {
            // At the stack bottom add the model call advisors.
            // They play the role of the last advisors in the advisor chain.
            this.advisors.add(ChatModelCallAdvisor.builder().chatModel(this.chatModel).build());
            this.advisors.add(ChatModelStreamAdvisor.builder().chatModel(this.chatModel).build());

            return DefaultAroundAdvisorChain.builder(this.observationRegistry)
                    .pushAll(this.advisors)
                    .templateRenderer(this.templateRenderer)
                    .build();
        }

    }

}
