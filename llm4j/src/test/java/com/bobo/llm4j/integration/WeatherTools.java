package com.bobo.llm4j.integration;

import com.bobo.llm4j.tool.annotation.Tool;
import com.bobo.llm4j.tool.annotation.ToolParam;

/**
 * 天气查询工具 - 用于集成测试
 */
public class WeatherTools {

    @Tool(name = "get_weather", description = "查询指定城市天气")
    public String getWeather(@ToolParam(description = "城市名称") String city) {
        return String.format("{\"city\":\"%s\",\"weather\":\"晴\",\"temp\":25,\"humidity\":60}", city);
    }
}
