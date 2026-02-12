package com.bobo.llm4j.tool;

import com.bobo.llm4j.tool.annotation.Tool;
import com.bobo.llm4j.tool.annotation.ToolParam;
import com.bobo.llm4j.tool.support.ToolCallbacks;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Tests for annotation-based tool callback creation.
 */
public class ToolCallbacksTest {

    @Test
    public void shouldBuildToolCallbacksFromAnnotatedObject() {
        List<ToolCallback> callbacks = ToolCallbacks.from(new DemoTools());
        Assert.assertEquals(2, callbacks.size());

        ToolCallback weather = findByName(callbacks, "weather");
        Assert.assertNotNull(weather);
        Assert.assertTrue(weather.getToolDefinition().getInputSchema().contains("\"city\""));
    }

    @Test
    public void shouldExecuteAnnotatedToolMethod() {
        List<ToolCallback> callbacks = ToolCallbacks.from(new DemoTools());
        ToolCallback weather = findByName(callbacks, "weather");

        String result = weather.call("{\"city\":\"Hangzhou\"}");
        Assert.assertEquals("weather:Hangzhou", result);
    }

    private ToolCallback findByName(List<ToolCallback> callbacks, String name) {
        for (ToolCallback callback : callbacks) {
            if (callback.getToolDefinition() != null && name.equals(callback.getToolDefinition().getName())) {
                return callback;
            }
        }
        return null;
    }

    static class DemoTools {

        @Tool(description = "Get weather by city")
        public String weather(@ToolParam(description = "City name") String city) {
            return "weather:" + city;
        }

        @Tool(name = "sum", description = "Calculate sum")
        public int add(int a, int b) {
            return a + b;
        }
    }
}
