package com.bobo.llm4j.tool.advisor;

import com.bobo.llm4j.chat.client.ChatClientRequest;
import com.bobo.llm4j.chat.client.ChatClientResponse;
import com.bobo.llm4j.chat.client.ChatOptions;
import com.bobo.llm4j.chat.client.advisor.CallAdvisor;
import com.bobo.llm4j.chat.client.advisor.CallAdvisorChain;
import com.bobo.llm4j.chat.entity.*;
import com.bobo.llm4j.chat.model.ChatModel;
import com.bobo.llm4j.tool.*;
import com.bobo.llm4j.tool.support.ToolCallbacks;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Advisor that intercepts the chat call to add tool-calling capabilities.
 * <p>
 * This advisor is self-contained: it holds the {@link ChatModel} reference and
 * all {@link ToolCallback} instances. When the model responds with tool-call
 * requests, the advisor executes the tools and re-invokes the model in a loop
 * until the model produces a final text answer or the maximum iteration count
 * is reached.
 * <p>
 * Because the entire tool-calling lifecycle is encapsulated here, <b>no
 * modifications to {@code ChatModel}, {@code ChatOptions}, or
 * {@code ChatClient}</b> are required.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * ToolCallingAdvisor advisor = ToolCallingAdvisor.builder()
 *         .chatModel(chatModel)
 *         .tools(new WeatherTools(), new SearchTools())
 *         .build();
 *
 * ChatClient client = ChatClient.builder(chatModel)
 *         .defaultAdvisors(advisor)
 *         .build();
 *
 * String answer = client.prompt()
 *         .user("What is the weather in Beijing?")
 *         .call()
 *         .content();
 * }</pre>
 */
@Slf4j
public class ToolCallingAdvisor implements CallAdvisor {

    private static final int DEFAULT_MAX_ITERATIONS = 8;
    private static final int DEFAULT_ORDER = Integer.MAX_VALUE - 10;

    private final ChatModel chatModel;
    private final List<ToolCallback> toolCallbacks;
    private final Object toolChoice;
    private final Boolean parallelToolCalls;
    private final Map<String, Object> toolContext;
    private final int maxIterations;
    private final int order;

    private ToolCallingAdvisor(Builder builder) {
        this.chatModel = builder.chatModel;
        this.toolCallbacks = Collections.unmodifiableList(builder.toolCallbacks);
        this.toolChoice = builder.toolChoice;
        this.parallelToolCalls = builder.parallelToolCalls;
        this.toolContext = builder.toolContext != null
                ? Collections.unmodifiableMap(builder.toolContext)
                : Collections.<String, Object>emptyMap();
        this.maxIterations = builder.maxIterations;
        this.order = builder.order;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        if (toolCallbacks == null || toolCallbacks.isEmpty()) {
            return chain.nextCall(request);
        }

        Map<String, ToolCallback> callbackIndex = new LinkedHashMap<String, ToolCallback>();
        List<ChatTool> chatTools = new ArrayList<ChatTool>();
        for (ToolCallback cb : toolCallbacks) {
            ToolDefinition def = cb.getToolDefinition();
            callbackIndex.put(def.getName(), cb);
            chatTools.add(ChatTool.fromDefinition(def));
        }

        Prompt prompt = buildPrompt(request, chatTools);
        List<Message> messages = new ArrayList<Message>(prompt.getMessages());
        ChatResponse response = null;

        for (int i = 0; i < maxIterations; i++) {
            Prompt currentPrompt = prompt.toBuilder()
                    .clearMessages()
                    .messages(messages)
                    .build();

            try {
                response = chatModel.call(currentPrompt);
            } catch (Exception e) {
                throw new RuntimeException("Failed to call chat model during tool calling", e);
            }

            if (response == null || !response.hasToolCalls()) {
                break;
            }

            appendToolRoundMessages(response, callbackIndex, messages);
        }

        return ChatClientResponse.builder()
                .chatResponse(response)
                .build();
    }

    // ---- prompt building ----

    private Prompt buildPrompt(ChatClientRequest request, List<ChatTool> chatTools) {
        List<Message> messages = request.getMessages() != null
                ? new ArrayList<Message>(request.getMessages())
                : new ArrayList<Message>();

        Prompt.PromptBuilder builder = Prompt.builder()
                .messages(messages)
                .tools(chatTools);

        if (toolChoice != null) {
            builder.toolChoice(toolChoice);
        }
        if (parallelToolCalls != null) {
            builder.parallelToolCalls(parallelToolCalls);
        }

        applyOptionsToPrompt(request.getOptions(), builder);

        return builder.build();
    }

    private static void applyOptionsToPrompt(ChatOptions options, Prompt.PromptBuilder builder) {
        if (options == null) {
            builder.model("default");
            return;
        }
        builder.model(options.getModel() != null ? options.getModel() : "default");

        if (options.getTemperature() != null) {
            builder.temperature(options.getTemperature());
        }
        if (options.getTopP() != null) {
            builder.topP(options.getTopP());
        }
        if (options.getMaxTokens() != null) {
            builder.maxCompletionTokens(options.getMaxTokens());
        }
        if (options.getFrequencyPenalty() != null) {
            builder.frequencyPenalty(options.getFrequencyPenalty());
        }
        if (options.getPresencePenalty() != null) {
            builder.presencePenalty(options.getPresencePenalty());
        }
        if (options.getStopSequences() != null) {
            builder.stop(options.getStopSequences());
        }
    }

    // ---- tool execution ----

    private void appendToolRoundMessages(ChatResponse response,
                                         Map<String, ToolCallback> callbackIndex,
                                         List<Message> messages) {
        for (Generation gen : response.getGenerations()) {
            Message msg = gen.getMessage();
            if (msg == null) {
                continue;
            }
            List<ToolCall> toolCalls = msg.getToolCalls();
            if (toolCalls == null || toolCalls.isEmpty()) {
                continue;
            }

            String textContent = msg.getContent() != null ? msg.getContent().getText() : null;
            messages.add(Message.withAssistantToolCalls(textContent, toolCalls));

            for (ToolCall toolCall : toolCalls) {
                String funcName = toolCall.getFunction().getName();
                ToolCallback callback = callbackIndex.get(funcName);
                if (callback == null) {
                    throw new ToolExecutionException("Tool callback not found: " + funcName);
                }

                String result;
                try {
                    if (!toolContext.isEmpty()) {
                        result = callback.call(toolCall.getFunction().getArguments(),
                                new ToolContext(new HashMap<String, Object>(toolContext)));
                    } else {
                        result = callback.call(toolCall.getFunction().getArguments());
                    }
                } catch (ToolExecutionException e) {
                    throw e;
                } catch (Exception e) {
                    result = "Tool execution error: " + e.getMessage();
                    log.warn("Tool '{}' execution failed", funcName, e);
                }

                messages.add(Message.withTool(toolCall.getId(), result));
            }
        }
    }

    // ---- Advisor metadata ----

    @Override
    public String getName() {
        return "ToolCallingAdvisor";
    }

    @Override
    public int getOrder() {
        return order;
    }

    // ---- Builder ----

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ChatModel chatModel;
        private List<ToolCallback> toolCallbacks = new ArrayList<ToolCallback>();
        private Object toolChoice;
        private Boolean parallelToolCalls;
        private Map<String, Object> toolContext;
        private int maxIterations = DEFAULT_MAX_ITERATIONS;
        private int order = DEFAULT_ORDER;

        public Builder chatModel(ChatModel chatModel) {
            this.chatModel = chatModel;
            return this;
        }

        public Builder toolCallbacks(List<ToolCallback> toolCallbacks) {
            this.toolCallbacks = toolCallbacks != null ? toolCallbacks : new ArrayList<ToolCallback>();
            return this;
        }

        /**
         * Convenience method: scan {@code @Tool}-annotated methods from the given objects.
         */
        public Builder tools(Object... toolObjects) {
            this.toolCallbacks = ToolCallbacks. from(toolObjects);
            return this;
        }

        public Builder toolChoice(Object toolChoice) {
            this.toolChoice = toolChoice;
            return this;
        }

        public Builder parallelToolCalls(Boolean parallelToolCalls) {
            this.parallelToolCalls = parallelToolCalls;
            return this;
        }

        public Builder toolContext(Map<String, Object> toolContext) {
            this.toolContext = toolContext;
            return this;
        }

        public Builder maxIterations(int maxIterations) {
            this.maxIterations = maxIterations;
            return this;
        }

        public Builder order(int order) {
            this.order = order;
            return this;
        }

        public ToolCallingAdvisor build() {
            if (chatModel == null) {
                throw new IllegalArgumentException("chatModel is required");
            }
            return new ToolCallingAdvisor(this);
        }
    }
}
