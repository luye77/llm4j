package com.bobo.llm4j.tool;

import com.bobo.llm4j.chat.client.ChatClientRequest;
import com.bobo.llm4j.chat.client.ChatClientResponse;
import com.bobo.llm4j.chat.client.ChatOptions;
import com.bobo.llm4j.chat.client.advisor.CallAdvisorChain;
import com.bobo.llm4j.chat.entity.*;
import com.bobo.llm4j.chat.model.ChatModel;
import com.bobo.llm4j.enums.MessageType;
import com.bobo.llm4j.http.Flux;
import com.bobo.llm4j.tool.annotation.Tool;
import com.bobo.llm4j.tool.annotation.ToolParam;
import com.bobo.llm4j.tool.advisor.ToolCallingAdvisor;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests for ToolCallingAdvisor: the Advisor-based tool-calling loop.
 */
public class ToolCallingAdvisorTest {

    /**
     * When the model responds with tool_calls, the advisor should execute the tool
     * and re-invoke the model with the tool result until a final text answer is returned.
     */
    @Test
    public void shouldExecuteToolCallLoopAndReturnFinalAnswer() {
        StubChatModel stubModel = new StubChatModel();
        stubModel.enqueue(buildToolCallResponse("call_1", "get_weather", "{\"city\":\"Hangzhou\"}"));
        stubModel.enqueue(buildTextResponse("The weather in Hangzhou is sunny."));

        ToolCallingAdvisor advisor = ToolCallingAdvisor.builder()
                .chatModel(stubModel)
                .tools(new WeatherTools())
                .build();

        ChatClientRequest request = new ChatClientRequest(
                Arrays.asList(new Message(MessageType.USER, "What is the weather?")),
                new StubChatOptions("test-model"),
                null, null);

        ChatClientResponse response = advisor.adviseCall(request, NOOP_CHAIN);

        Assert.assertNotNull(response.chatResponse());
        Assert.assertFalse(response.chatResponse().hasToolCalls());
        Assert.assertEquals(2, stubModel.getCallCount());

        Prompt lastPrompt = stubModel.getLastPrompt();
        Assert.assertNotNull(lastPrompt.getTools());
        Assert.assertFalse(lastPrompt.getTools().isEmpty());

        List<Message> msgs = lastPrompt.getMessages();
        Assert.assertTrue(msgs.size() >= 3);
        Assert.assertEquals(MessageType.TOOL.getRole(), msgs.get(msgs.size() - 1).getRole());
        Assert.assertEquals("call_1", msgs.get(msgs.size() - 1).getToolCallId());
        Assert.assertTrue(msgs.get(msgs.size() - 1).getContent().getText().contains("Hangzhou"));
    }

    /**
     * When no tool callbacks are configured, the advisor should pass through to the chain.
     */
    @Test
    public void shouldPassThroughWhenNoToolsConfigured() {
        StubChatModel stubModel = new StubChatModel();

        ToolCallingAdvisor advisor = ToolCallingAdvisor.builder()
                .chatModel(stubModel)
                .toolCallbacks(Collections.<ToolCallback>emptyList())
                .build();

        final boolean[] chainCalled = {false};
        CallAdvisorChain chain = new CallAdvisorChain() {
            @Override
            public ChatClientResponse nextCall(ChatClientRequest request) {
                chainCalled[0] = true;
                return ChatClientResponse.builder()
                        .chatResponse(buildTextResponse("from chain"))
                        .build();
            }
        };

        ChatClientRequest request = new ChatClientRequest(
                Arrays.asList(new Message(MessageType.USER, "hi")),
                null, null, null);

        ChatClientResponse result = advisor.adviseCall(request, chain);
        Assert.assertTrue(chainCalled[0]);
        Assert.assertNotNull(result.chatResponse());
        Assert.assertEquals(0, stubModel.getCallCount());
    }

    /**
     * Max iterations should be honored to prevent infinite loops.
     */
    @Test
    public void shouldStopAfterMaxIterations() {
        StubChatModel stubModel = new StubChatModel();
        for (int i = 0; i < 5; i++) {
            stubModel.enqueue(buildToolCallResponse("call_" + i, "get_weather", "{\"city\":\"Beijing\"}"));
        }

        ToolCallingAdvisor advisor = ToolCallingAdvisor.builder()
                .chatModel(stubModel)
                .tools(new WeatherTools())
                .maxIterations(3)
                .build();

        ChatClientRequest request = new ChatClientRequest(
                Arrays.asList(new Message(MessageType.USER, "loop")),
                new StubChatOptions("test-model"),
                null, null);

        ChatClientResponse result = advisor.adviseCall(request, NOOP_CHAIN);

        Assert.assertNotNull(result.chatResponse());
        Assert.assertEquals(3, stubModel.getCallCount());
    }

    /**
     * ToolContext should be forwarded to the tool callback.
     */
    @Test
    public void shouldForwardToolContext() {
        StubChatModel stubModel = new StubChatModel();
        stubModel.enqueue(buildToolCallResponse("ctx_1", "greet", "{\"name\":\"Alice\"}"));
        stubModel.enqueue(buildTextResponse("Done"));

        Map<String, Object> ctx = new HashMap<String, Object>();
        ctx.put("tenant", "acme");

        ToolCallingAdvisor advisor = ToolCallingAdvisor.builder()
                .chatModel(stubModel)
                .tools(new GreetTools())
                .toolContext(ctx)
                .build();

        ChatClientRequest request = new ChatClientRequest(
                Arrays.asList(new Message(MessageType.USER, "greet")),
                new StubChatOptions("test-model"),
                null, null);

        ChatClientResponse result = advisor.adviseCall(request, NOOP_CHAIN);
        Assert.assertNotNull(result.chatResponse());

        Prompt lastPrompt = stubModel.getLastPrompt();
        List<Message> msgs = lastPrompt.getMessages();
        Message toolMsg = msgs.get(msgs.size() - 1);
        Assert.assertEquals(MessageType.TOOL.getRole(), toolMsg.getRole());
        Assert.assertTrue(toolMsg.getContent().getText().contains("Alice@acme"));
    }

    // ---- test tool classes ----

    static class WeatherTools {
        @Tool(name = "get_weather", description = "Get weather by city")
        public String getWeather(@ToolParam(description = "City name") String city) {
            return "Weather in " + city + ": sunny, 25°C";
        }
    }

    static class GreetTools {
        @Tool(name = "greet", description = "Greet a person")
        public String greet(String name, ToolContext context) {
            String tenant = context != null && context.getContext() != null
                    ? String.valueOf(context.getContext().get("tenant"))
                    : "unknown";
            return "Hello " + name + "@" + tenant;
        }
    }

    // ---- helpers ----

    private static ChatResponse buildToolCallResponse(String id, String toolName, String args) {
        ToolCall call = ToolCall.builder()
                .id(id)
                .type("function")
                .function(ToolCall.Function.builder().name(toolName).arguments(args).build())
                .build();

        Message assistant = Message.withAssistantToolCalls(null, Arrays.asList(call));
        Generation generation = new Generation();
        generation.setMessage(assistant);

        ChatResponse response = new ChatResponse();
        response.setGenerations(Arrays.asList(generation));
        return response;
    }

    private static ChatResponse buildTextResponse(String text) {
        Message assistant = Message.withAssistant(text);
        Generation generation = new Generation();
        generation.setMessage(assistant);
        generation.setFinishReason("stop");

        ChatResponse response = new ChatResponse();
        response.setGenerations(Arrays.asList(generation));
        return response;
    }

    private static final CallAdvisorChain NOOP_CHAIN = new CallAdvisorChain() {
        @Override
        public ChatClientResponse nextCall(ChatClientRequest request) {
            return ChatClientResponse.builder().build();
        }
    };

    // ---- stub implementations ----

    static class StubChatModel implements ChatModel {
        private final Queue<ChatResponse> responses = new LinkedList<ChatResponse>();
        private final AtomicInteger callCount = new AtomicInteger(0);
        private volatile Prompt lastPrompt;

        void enqueue(ChatResponse response) {
            responses.add(response);
        }

        int getCallCount() {
            return callCount.get();
        }

        Prompt getLastPrompt() {
            return lastPrompt;
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            callCount.incrementAndGet();
            lastPrompt = prompt;
            ChatResponse next = responses.poll();
            return next != null ? next : buildTextResponse("default");
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            throw new UnsupportedOperationException();
        }

        private static ChatResponse buildTextResponse(String text) {
            Message assistant = Message.withAssistant(text);
            Generation gen = new Generation();
            gen.setMessage(assistant);
            ChatResponse r = new ChatResponse();
            r.setGenerations(Arrays.asList(gen));
            return r;
        }
    }

    static class StubChatOptions implements ChatOptions {
        private final String model;

        StubChatOptions(String model) {
            this.model = model;
        }

        @Override public String getModel() { return model; }
        @Override public Float getFrequencyPenalty() { return null; }
        @Override public Integer getMaxTokens() { return null; }
        @Override public Float getPresencePenalty() { return null; }
        @Override public List<String> getStopSequences() { return null; }
        @Override public Float getTemperature() { return null; }
        @Override public Integer getTopK() { return null; }
        @Override public Float getTopP() { return null; }
        @Override public ChatOptions copy() { return new StubChatOptions(model); }
    }
}
