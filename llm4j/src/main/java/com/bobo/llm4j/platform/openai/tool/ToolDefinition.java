package com.bobo.llm4j.platform.openai.tool;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * ToolDefinition - 工具定义 (对应Spring AI的ToolDefinition/FunctionCallback)
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolDefinition {

    /**
     * 工具类型，目前为"function"
     */
    private String type;
    private Function function;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Function {

        /**
         * 函数名称
         */
        private String name;

        /**
         * 函数描述
         */
        private String description;

        /**
         * 函数参数
         */
        private Parameter parameters;

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class Parameter {

            private String type = "object";

            /**
             * 参数属性
             */
            private Map<String, Property> properties;

            /**
             * 必须的参数
             */
            private List<String> required;
        }

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class Property {
            private String type;
            private String description;

            @JsonProperty("enum")
            private List<String> enumValues;

            /**
             * 数组元素类型定义
             */
            private Property items;
        }
    }
}

