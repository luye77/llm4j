package com.bobo.llm4j.function;

import com.bobo.llm4j.annotation.Tool;
import com.bobo.llm4j.annotation.ToolParam;
import com.bobo.llm4j.annotation.ToolRequest;
import com.bobo.llm4j.base.BaseTest;
import com.bobo.llm4j.http.StreamingResponseHandler;
import com.bobo.llm4j.platform.openai.chat.entity.ChatResponse;
import com.bobo.llm4j.platform.openai.chat.entity.Message;
import com.bobo.llm4j.platform.openai.chat.entity.Prompt;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.function.Function;

/**
 * 函数调用测试类
 * <p>
 * 测试框架的函数调用功能，包括：
 * <ul>
 *     <li>同步函数调用</li>
 *     <li>流式函数调用</li>
 *     <li>多函数选择调用</li>
 *     <li>并行函数调用</li>
 * </ul>
 * </p>
 *
 * @author bobo
 * @since 1.4.3
 */
@Slf4j
public class FunctionCallTest extends BaseTest {

    /**
     * 测试同步函数调用
     *
     * @throws Exception 调用异常
     */
    @Test
    public void testSyncFunctionCall() throws Exception {
        log.info("=== 测试同步函数调用 ===");

        Prompt prompt = Prompt.builder()
                .model(CHAT_MODEL)
                .message(Message.withUser("查询北京明天的天气"))
                .functions("queryWeather")
                .build();

        ObjectMapper mapper = new ObjectMapper();
        log.info("请求JSON: {}", mapper.writeValueAsString(prompt));

        ChatResponse response = chatModel.call(prompt);

        String content = response.getGenerations().get(0).getMessage().getContent().getText();
        log.info("助手回复: {}", content);
        log.info("Token使用: {}", response.getUsage());
    }

    /**
     * 测试流式函数调用
     *
     * @throws Exception 调用异常
     */
    @Test
    public void testStreamFunctionCall() throws Exception {
        log.info("=== 测试流式函数调用 ===");

        Prompt prompt = Prompt.builder()
                .model(CHAT_MODEL)
                .message(Message.withUser("查询上海后天的天气怎么样"))
                .functions("queryWeather")
                .build();

        StreamingResponseHandler handler = new StreamingResponseHandler() {
            @Override
            protected void send() {
                String chunk = this.getCurrStr();
                if (chunk != null && !chunk.isEmpty()) {
                    System.out.print(chunk);
                }
            }
        };
        handler.setShowToolArgs(true);

        chatModel.stream(prompt, handler);

        System.out.println();
        log.info("完整输出: {}", handler.getOutput().toString());
        log.info("Token使用: {}", handler.getUsage());
    }

    /**
     * 测试多函数选择调用
     *
     * @throws Exception 调用异常
     */
    @Test
    public void testMultipleFunctionsCall() throws Exception {
        log.info("=== 测试多函数选择调用 ===");

        Prompt prompt = Prompt.builder()
                .model(CHAT_MODEL)
                .message(Message.withUser("帮我计算 25 * 4 等于多少"))
                .functions("queryWeather", "calculator", "searchInfo")
                .parallelToolCalls(true)
                .build();

        ChatResponse response = chatModel.call(prompt);

        String content = response.getGenerations().get(0).getMessage().getContent().getText();
        log.info("助手回复: {}", content);
    }

    /**
     * 测试天气查询函数调用
     *
     * @throws Exception 调用异常
     */
    @Test
    public void testWeatherQueryFunction() throws Exception {
        log.info("=== 测试天气查询函数调用 ===");

        Prompt prompt = Prompt.builder()
                .model(CHAT_MODEL)
                .message(Message.withUser("深圳今天天气怎么样？适合户外运动吗？"))
                .functions("queryWeather")
                .build();

        ChatResponse response = chatModel.call(prompt);

        String content = response.getGenerations().get(0).getMessage().getContent().getText();
        log.info("助手回复: {}", content);
    }

    /**
     * 测试计算器函数调用
     *
     * @throws Exception 调用异常
     */
    @Test
    public void testCalculatorFunction() throws Exception {
        log.info("=== 测试计算器函数调用 ===");

        Prompt prompt = Prompt.builder()
                .model(CHAT_MODEL)
                .message(Message.withUser("请帮我计算 (100 + 50) 的结果"))
                .functions("calculator")
                .build();

        ChatResponse response = chatModel.call(prompt);

        String content = response.getGenerations().get(0).getMessage().getContent().getText();
        log.info("助手回复: {}", content);
    }

    // ==================== 工具类定义 ====================

    /**
     * 天气查询工具
     */
    @Tool(name = "queryWeather", description = "查询指定城市和日期的天气信息")
    public static class QueryWeatherTool implements Function<QueryWeatherTool.Request, String> {

        /**
         * 天气查询请求
         */
        @Data
        @ToolRequest
        public static class Request {
            /**
             * 城市名称
             */
            @ToolParam(description = "城市名称，例如：北京、上海")
            private String city;

            /**
             * 查询日期
             */
            @ToolParam(description = "查询日期，格式：YYYY-MM-DD")
            private String date;
        }

        @Override
        public String apply(Request request) {
            return String.format(
                    "城市: %s, 日期: %s, 天气: 晴转多云, 温度: 15-25°C, 湿度: 45%%",
                    request.getCity(),
                    request.getDate()
            );
        }
    }

    /**
     * 计算器工具
     */
    @Tool(name = "calculator", description = "执行数学计算")
    public static class CalculatorTool implements Function<CalculatorTool.Request, String> {

        /**
         * 计算器请求
         */
        @Data
        @ToolRequest
        public static class Request {
            /**
             * 数学表达式
             */
            @ToolParam(description = "数学表达式，例如：2+3, 10*5, 100/4")
            private String expression;
        }

        @Override
        public String apply(Request request) {
            try {
                String expr = request.getExpression().replaceAll("\\s+", "");
                double result = evaluateExpression(expr);
                return String.format("计算结果: %s = %.2f", request.getExpression(), result);
            } catch (Exception e) {
                return "计算错误: " + e.getMessage();
            }
        }

        /**
         * 简单表达式计算
         *
         * @param expr 表达式
         * @return 计算结果
         */
        private double evaluateExpression(String expr) {
            if (expr.contains("+")) {
                String[] parts = expr.split("\\+");
                return Double.parseDouble(parts[0]) + Double.parseDouble(parts[1]);
            } else if (expr.contains("-")) {
                String[] parts = expr.split("-");
                return Double.parseDouble(parts[0]) - Double.parseDouble(parts[1]);
            } else if (expr.contains("*")) {
                String[] parts = expr.split("\\*");
                return Double.parseDouble(parts[0]) * Double.parseDouble(parts[1]);
            } else if (expr.contains("/")) {
                String[] parts = expr.split("/");
                return Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]);
            }
            throw new IllegalArgumentException("无法解析表达式: " + expr);
        }
    }

    /**
     * 搜索信息工具
     */
    @Tool(name = "searchInfo", description = "搜索相关信息")
    public static class SearchInfoTool implements Function<SearchInfoTool.Request, String> {

        /**
         * 搜索请求
         */
        @Data
        @ToolRequest
        public static class Request {
            /**
             * 搜索关键词
             */
            @ToolParam(description = "搜索关键词")
            private String keyword;

            /**
             * 结果数量限制
             */
            @ToolParam(description = "搜索结果数量限制", required = false)
            private Integer limit;
        }

        @Override
        public String apply(Request request) {
            int resultLimit = request.getLimit() != null ? request.getLimit() : 3;
            return String.format("搜索关键词: %s, 找到 %d 条相关结果", request.getKeyword(), resultLimit);
        }
    }
}
