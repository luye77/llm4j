package com.bobo.llm4j.tool;

import com.bobo.llm4j.chat.entity.ChatResponse;
import com.bobo.llm4j.chat.entity.Generation;
import com.bobo.llm4j.chat.entity.Message;
import com.bobo.llm4j.chat.entity.Prompt;
import com.bobo.llm4j.chat.entity.ToolCall;
import com.bobo.llm4j.enums.MessageType;
import com.bobo.llm4j.tool.annotation.Tool;
import com.bobo.llm4j.tool.support.ToolCallbacks;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Tests for DefaultToolCallingManager execution flow.
 */
public class DefaultToolCallingManagerTest {

    @Test
    public void shouldResolveDefinitionsAndAppendToolMessages() {
        DefaultToolCallingManager manager = new DefaultToolCallingManager();
        List<ToolCallback> callbacks = ToolCallbacks.from(new DemoTools());

        Prompt prompt = Prompt.builder()
                .model("qwen-plus")
                .messages(Arrays.asList(new Message(MessageType.USER, "query weather")))
                .toolCallbacks(callbacks)
                .build();

        List<ToolDefinition> definitions = manager.resolveToolDefinitions(prompt);
        Assert.assertEquals(1, definitions.size());
        Assert.assertEquals("get_weather", definitions.get(0).getName());

        ChatResponse response = buildToolCallResponse("call_1", "get_weather", "{\"city\":\"Hangzhou\"}");
        ToolExecutionResult result = manager.executeToolCalls(prompt, response);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getConversationHistory());
        Assert.assertEquals(3, result.getConversationHistory().size());

        Message toolMsg = result.getConversationHistory().get(2);
        Assert.assertEquals(MessageType.TOOL.getRole(), toolMsg.getRole());
        Assert.assertEquals("call_1", toolMsg.getToolCallId());
        Assert.assertEquals("weather:Hangzhou", toolMsg.getContent().getText());
    }

    @Test(expected = IllegalStateException.class)
    public void shouldFailWhenToolCallbackNotFound() {
        DefaultToolCallingManager manager = new DefaultToolCallingManager();

        Prompt prompt = Prompt.builder()
                .model("qwen-plus")
                .messages(Arrays.asList(new Message(MessageType.USER, "query weather")))
                .build();

        ChatResponse response = buildToolCallResponse("call_1", "get_weather", "{\"city\":\"Hangzhou\"}");
        manager.executeToolCalls(prompt, response);
    }

    private ChatResponse buildToolCallResponse(String id, String toolName, String argumentsJson) {
        ToolCall call = ToolCall.builder()
                .id(id)
                .type("function")
                .function(ToolCall.Function.builder().name(toolName).arguments(argumentsJson).build())
                .build();

        Message assistant = Message.withAssistantToolCalls(null, Arrays.asList(call));
        Generation generation = new Generation();
        generation.setMessage(assistant);

        ChatResponse response = new ChatResponse();
        response.setGenerations(Arrays.asList(generation));
        return response;
    }

    static class DemoTools {
        @Tool(name = "get_weather", description = "Get weather by city")
        public String getWeather(String city) {
            return "weather:" + city;
        }
    }
}
