package com.bobo.llm4j.tool;

import com.bobo.llm4j.annotation.Tool;
import com.bobo.llm4j.annotation.ToolParam;
import com.bobo.llm4j.annotation.ToolRequest;
import com.bobo.llm4j.platform.openai.tool.ToolDefinition;
import com.bobo.llm4j.utils.ToolUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * 工具类测试类
 * <p>
 * 测试ToolUtil工具类的功能，包括：
 * <ul>
 *     <li>工具Schema生成</li>
 *     <li>工具定义获取</li>
 *     <li>工具调用执行</li>
 *     <li>批量工具获取</li>
 * </ul>
 * </p>
 *
 * @author bobo
 * @since 1.4.3
 */
@Slf4j
public class ToolUtilTest {

    /**
     * 测试获取函数定义
     */
    @Test
    public void testGetFunctionDefinition() {
        log.info("=== 测试获取函数定义 ===");

        ToolDefinition.Function function = ToolUtil.getFunctionDefinition("queryWeather");

        Assert.assertNotNull("函数定义不应为空", function);
        Assert.assertEquals("函数名称验证", "queryWeather", function.getName());
        Assert.assertNotNull("函数描述不应为空", function.getDescription());
        Assert.assertNotNull("函数参数不应为空", function.getParameters());

        log.info("函数名称: {}", function.getName());
        log.info("函数描述: {}", function.getDescription());
        log.info("函数参数: {}", function.getParameters());
    }

    /**
     * 测试获取工具定义
     */
    @Test
    public void testGetToolDefinition() {
        log.info("=== 测试获取工具定义 ===");

        ToolDefinition tool = ToolUtil.getToolDefinition("calculator");

        Assert.assertNotNull("工具定义不应为空", tool);
        Assert.assertEquals("工具类型验证", "function", tool.getType());
        Assert.assertNotNull("工具函数不应为空", tool.getFunction());

        log.info("工具类型: {}", tool.getType());
        log.info("工具函数: {}", tool.getFunction());
    }

    /**
     * 测试批量获取工具
     */
    @Test
    public void testGetAllTools() {
        log.info("=== 测试批量获取工具 ===");

        List<String> functionNames = Arrays.asList("queryWeather", "calculator", "searchInfo");

        List<ToolDefinition> tools = ToolUtil.getAllTools(functionNames);

        Assert.assertNotNull("工具列表不应为空", tools);
        log.info("请求工具数: {}, 获取工具数: {}", functionNames.size(), tools.size());

        tools.forEach(tool -> {
            log.info("工具: {} - {}", tool.getFunction().getName(), tool.getFunction().getDescription());
        });
    }

    /**
     * 测试工具调用
     */
    @Test
    public void testToolInvoke() {
        log.info("=== 测试工具调用 ===");

        // 调用天气查询工具
        String weatherArgs = "{\"city\":\"深圳\",\"date\":\"2024-12-25\"}";
        String weatherResult = ToolUtil.invoke("queryWeather", weatherArgs);

        Assert.assertNotNull("调用结果不应为空", weatherResult);
        log.info("天气查询结果: {}", weatherResult);

        // 调用计算器工具
        String calcArgs = "{\"expression\":\"100 + 200\"}";
        String calcResult = ToolUtil.invoke("calculator", calcArgs);

        Assert.assertNotNull("调用结果不应为空", calcResult);
        log.info("计算器结果: {}", calcResult);
    }

    /**
     * 测试空参数获取工具
     */
    @Test
    public void testGetToolsWithEmptyList() {
        log.info("=== 测试空参数获取工具 ===");

        List<ToolDefinition> tools = ToolUtil.getAllTools(null);
        Assert.assertNotNull("返回列表不应为空", tools);
        Assert.assertTrue("返回列表应为空列表", tools.isEmpty());

        tools = ToolUtil.getAllTools(Arrays.asList());
        Assert.assertNotNull("返回列表不应为空", tools);
        Assert.assertTrue("返回列表应为空列表", tools.isEmpty());

        log.info("空参数测试通过");
    }

    /**
     * 测试不存在的函数定义
     */
    @Test
    public void testNonExistentFunction() {
        log.info("=== 测试不存在的函数定义 ===");

        ToolDefinition.Function function = ToolUtil.getFunctionDefinition("nonExistentFunction");
        Assert.assertNull("不存在的函数应返回null", function);

        ToolDefinition tool = ToolUtil.getToolDefinition("nonExistentFunction");
        Assert.assertNull("不存在的工具应返回null", tool);

        log.info("不存在函数测试通过");
    }

    /**
     * 测试空函数名
     */
    @Test
    public void testEmptyFunctionName() {
        log.info("=== 测试空函数名 ===");

        ToolDefinition.Function function1 = ToolUtil.getFunctionDefinition(null);
        Assert.assertNull("null函数名应返回null", function1);

        ToolDefinition.Function function2 = ToolUtil.getFunctionDefinition("");
        Assert.assertNull("空字符串函数名应返回null", function2);

        ToolDefinition.Function function3 = ToolUtil.getFunctionDefinition("   ");
        Assert.assertNull("空白字符串函数名应返回null", function3);

        log.info("空函数名测试通过");
    }

    // ==================== 工具类定义（用于测试） ====================

    /**
     * 天气查询工具
     */
    @Tool(name = "queryWeather", description = "查询指定城市和日期的天气信息")
    public static class QueryWeatherTool implements Function<QueryWeatherTool.Request, String> {

        /**
         * 请求参数
         */
        @Data
        @ToolRequest
        public static class Request {
            @ToolParam(description = "城市名称")
            private String city;

            @ToolParam(description = "查询日期")
            private String date;
        }

        @Override
        public String apply(Request request) {
            return String.format("城市: %s, 日期: %s, 天气: 晴", request.getCity(), request.getDate());
        }
    }

    /**
     * 计算器工具
     */
    @Tool(name = "calculator", description = "执行数学计算")
    public static class CalculatorTool implements Function<CalculatorTool.Request, String> {

        /**
         * 请求参数
         */
        @Data
        @ToolRequest
        public static class Request {
            @ToolParam(description = "数学表达式")
            private String expression;
        }

        @Override
        public String apply(Request request) {
            return "计算结果: " + request.getExpression();
        }
    }

    /**
     * 搜索信息工具
     */
    @Tool(name = "searchInfo", description = "搜索相关信息")
    public static class SearchInfoTool implements Function<SearchInfoTool.Request, String> {

        /**
         * 请求参数
         */
        @Data
        @ToolRequest
        public static class Request {
            @ToolParam(description = "搜索关键词")
            private String keyword;

            @ToolParam(description = "结果数量限制", required = false)
            private Integer limit;
        }

        @Override
        public String apply(Request request) {
            return "搜索结果: " + request.getKeyword();
        }
    }
}
