package com.bobo.llm4j.integration;

import com.bobo.llm4j.tool.annotation.Tool;
import com.bobo.llm4j.tool.annotation.ToolParam;

/**
 * 计算器工具 - 用于集成测试
 */
public class CalculatorTools {

    @Tool(name = "add", description = "计算两数之和")
    public int add(@ToolParam(description = "第一个数") int a,
                   @ToolParam(description = "第二个数") int b) {
        return a + b;
    }
}
