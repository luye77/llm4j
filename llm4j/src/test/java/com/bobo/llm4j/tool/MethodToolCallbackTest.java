package com.bobo.llm4j.tool;

import com.bobo.llm4j.tool.method.MethodToolCallback;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests for MethodToolCallback argument conversion and context support.
 */
public class MethodToolCallbackTest {

    @Test
    public void shouldInjectToolContextIntoMethod() throws Exception {
        DemoToolBean bean = new DemoToolBean();
        Method method = DemoToolBean.class.getDeclaredMethod("greet", String.class, ToolContext.class);

        MethodToolCallback callback = new MethodToolCallback(
                ToolDefinition.builder()
                        .name("greet")
                        .description("Greet with tenant context")
                        .inputSchema("{\"type\":\"object\"}")
                        .build(),
                ToolMetadata.builder().build(),
                method,
                bean
        );

        Map<String, Object> ctx = new HashMap<String, Object>();
        ctx.put("tenant", "demo");

        String result = callback.call("{\"name\":\"bobo\"}", new ToolContext(ctx));
        Assert.assertEquals("hello bobo@demo", result);
    }

    @Test
    public void shouldSerializeObjectReturnValueAsJson() throws Exception {
        DemoToolBean bean = new DemoToolBean();
        Method method = DemoToolBean.class.getDeclaredMethod("stats", int.class, int.class);

        MethodToolCallback callback = new MethodToolCallback(
                ToolDefinition.builder()
                        .name("stats")
                        .description("Return object")
                        .inputSchema("{\"type\":\"object\"}")
                        .build(),
                ToolMetadata.builder().build(),
                method,
                bean
        );

        String result = callback.call("{\"a\":2,\"b\":3}");
        Assert.assertTrue(result.contains("\"sum\":5"));
        Assert.assertTrue(result.contains("\"max\":3"));
    }

    static class DemoToolBean {

        public String greet(String name, ToolContext context) {
            Object tenant = context != null && context.getContext() != null ? context.getContext().get("tenant") : "unknown";
            return "hello " + name + "@" + tenant;
        }

        public Map<String, Integer> stats(int a, int b) {
            Map<String, Integer> data = new HashMap<String, Integer>();
            data.put("sum", a + b);
            data.put("max", Math.max(a, b));
            return data;
        }
    }
}
