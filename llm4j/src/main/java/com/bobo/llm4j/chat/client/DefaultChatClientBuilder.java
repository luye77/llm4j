package com.bobo.llm4j.chat.client;

import cn.hutool.core.lang.Assert;
import com.bobo.llm4j.chat.client.advisor.Advisor;
import com.bobo.llm4j.chat.client.observe.ChatClientObservationConvention;
import com.bobo.llm4j.chat.client.observe.ObservationRegistry;
import com.bobo.llm4j.chat.model.ChatModel;
import com.bobo.llm4j.chat.util.StringUtils;
import com.bobo.llm4j.chat.entity.Media;
import com.bobo.llm4j.chat.entity.Message;
import com.bobo.llm4j.template.TemplateRenderer;
import com.google.common.collect.Lists;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Default implementation of ChatClient.Builder
 */
public class DefaultChatClientBuilder implements ChatClient.Builder {
    
    private final ChatModel chatModel;
    private final ObservationRegistry observationRegistry;
    private final ChatClientObservationConvention observationConvention;
    
    private final List<Advisor> advisors = new ArrayList<>();
    private final Map<String, Object> advisorParams = new HashMap<>();
    private final Map<String, Object> userParams = new HashMap<>();
    private final Map<String, Object> systemParams = new HashMap<>();
    private final List<Message> messages = new ArrayList<>();
    private final List<Media> media = new ArrayList<>();
    
    private String userText;
    private String systemText;
    private ChatOptions chatOptions;
    private TemplateRenderer templateRenderer;
    
    public DefaultChatClientBuilder(ChatModel chatModel, 
                                   ObservationRegistry observationRegistry,
                                   ChatClientObservationConvention observationConvention) {
        Assert.notNull(chatModel, "chatModel cannot be null");
        Assert.notNull(observationRegistry, "observationRegistry cannot be null");
        
        this.chatModel = chatModel;
        this.observationRegistry = observationRegistry;
        this.observationConvention = observationConvention;
    }
    
    @Override
    public ChatClient.Builder defaultAdvisors(Advisor... advisors) {
        Assert.notNull(advisors, "advisors cannot be null");
        Assert.noNullElements(advisors, "advisors cannot contain null elements");
        this.advisors.addAll(Lists.newArrayList(advisors));
        return this;
    }
    
    @Override
    public ChatClient.Builder defaultAdvisors(Consumer<ChatClient.AdvisorSpec> advisorSpecConsumer) {
        Assert.notNull(advisorSpecConsumer, "advisorSpecConsumer cannot be null");
        DefaultChatClient.DefaultAdvisorSpec spec = new DefaultChatClient.DefaultAdvisorSpec();
        advisorSpecConsumer.accept(spec);
        this.advisors.addAll(spec.getAdvisors());
        this.advisorParams.putAll(spec.getParams());
        return this;
    }
    
    @Override
    public ChatClient.Builder defaultAdvisors(List<Advisor> advisors) {
        Assert.notNull(advisors, "advisors cannot be null");
        this.advisors.addAll(advisors);
        return this;
    }
    
    @Override
    public ChatClient.Builder defaultOptions(ChatOptions chatOptions) {
        this.chatOptions = chatOptions;
        return this;
    }
    
    @Override
    public ChatClient.Builder defaultUser(String text) {
        this.userText = text;
        return this;
    }
    
    @Override
    public ChatClient.Builder defaultUser(Resource text, Charset charset) {
        Assert.notNull(text, "text cannot be null");
        Assert.notNull(charset, "charset cannot be null");
        try {
            this.userText = text.getContentAsString(charset);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }
    
    @Override
    public ChatClient.Builder defaultUser(Resource text) {
        return defaultUser(text, Charset.defaultCharset());
    }
    
    @Override
    public ChatClient.Builder defaultUser(Consumer<ChatClient.PromptUserSpec> userSpecConsumer) {
        Assert.notNull(userSpecConsumer, "userSpecConsumer cannot be null");
        DefaultChatClient.DefaultPromptUserSpec spec = new DefaultChatClient.DefaultPromptUserSpec();
        userSpecConsumer.accept(spec);
        if (StringUtils.hasText(spec.text())) {
            this.userText = spec.text();
        }
        this.userParams.putAll(spec.params());
        this.media.addAll(spec.media());
        return this;
    }
    
    @Override
    public ChatClient.Builder defaultSystem(String text) {
        this.systemText = text;
        return this;
    }
    
    @Override
    public ChatClient.Builder defaultSystem(Resource text, Charset charset) {
        Assert.notNull(text, "text cannot be null");
        Assert.notNull(charset, "charset cannot be null");
        try {
            this.systemText = text.getContentAsString(charset);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }
    
    @Override
    public ChatClient.Builder defaultSystem(Resource text) {
        return defaultSystem(text, Charset.defaultCharset());
    }
    
    @Override
    public ChatClient.Builder defaultSystem(Consumer<ChatClient.PromptSystemSpec> systemSpecConsumer) {
        Assert.notNull(systemSpecConsumer, "systemSpecConsumer cannot be null");
        DefaultChatClient.DefaultPromptSystemSpec spec = new DefaultChatClient.DefaultPromptSystemSpec();
        systemSpecConsumer.accept(spec);
        if (StringUtils.hasText(spec.text())) {
            this.systemText = spec.text();
        }
        this.systemParams.putAll(spec.params());
        return this;
    }
    
    @Override
    public ChatClient.Builder defaultTemplateRenderer(TemplateRenderer templateRenderer) {
        this.templateRenderer = templateRenderer;
        return this;
    }
    
    @Override
    public ChatClient.Builder clone() {
        DefaultChatClientBuilder clone = new DefaultChatClientBuilder(
            this.chatModel, this.observationRegistry, this.observationConvention);
        
        clone.advisors.addAll(this.advisors);
        clone.advisorParams.putAll(this.advisorParams);
        clone.userParams.putAll(this.userParams);
        clone.systemParams.putAll(this.systemParams);
        clone.messages.addAll(this.messages);
        clone.media.addAll(this.media);
        
        clone.userText = this.userText;
        clone.systemText = this.systemText;
        clone.chatOptions = this.chatOptions;
        clone.templateRenderer = this.templateRenderer;
        
        return clone;
    }
    
    @Override
    public ChatClient build() {
        DefaultChatClient.DefaultChatClientRequestSpec requestSpec = 
            new DefaultChatClient.DefaultChatClientRequestSpec(
                this.chatModel,
                this.userText,
                this.userParams,
                this.systemText,
                this.systemParams,
                this.messages,
                this.media,
                this.chatOptions,
                this.advisors,
                this.advisorParams,
                this.observationRegistry,
                this.observationConvention,
                this.templateRenderer
            );
        
        return new DefaultChatClient(requestSpec);
    }
    
    // Additional helper methods
    
    public void addMessages(List<Message> messages) {
        if (messages != null) {
            this.messages.addAll(messages);
        }
    }
}
