package com.bobo.llm4j.platform.qwen.chat;

import com.bobo.llm4j.chat.client.ChatOptions;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * QwenChatOptions - 千问模型配置选项 (OpenAI兼容模式)
 * <p>
 * 千问(Qwen)支持OpenAI兼容的API格式，因此配置项与OpenAI类似。
 * 支持的模型包括: qwen-turbo, qwen-plus, qwen-max 等。
 * </p>
 * 
 * @see ChatOptions
 * @see <a href="https://help.aliyun.com/zh/dashscope/developer-reference/compatibility-of-openai-with-dashscope">千问OpenAI兼容文档</a>
 * @author bobo
 * @since 1.0.0
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QwenChatOptions implements ChatOptions {
    
    // ========== Portable ChatOptions Fields ==========
    
    /**
     * 模型名称 (e.g., "qwen-turbo", "qwen-plus", "qwen-max")
     */
    @JsonProperty("model")
    private String model;
    
    /**
     * 频率惩罚 [-2.0, 2.0]
     */
    @JsonProperty("frequency_penalty")
    private Float frequencyPenalty;
    
    /**
     * 最大生成token数
     */
    @JsonProperty("max_tokens")
    private Integer maxTokens;
    
    /**
     * 存在惩罚 [-2.0, 2.0]
     */
    @JsonProperty("presence_penalty")
    private Float presencePenalty;
    
    /**
     * 停止词序列
     */
    @JsonProperty("stop")
    private List<String> stopSequences;
    
    /**
     * 采样温度 [0.0, 2.0]
     */
    @JsonProperty("temperature")
    private Float temperature;
    
    /**
     * Top-K 采样
     */
    @JsonProperty("top_k")
    private Integer topK;
    
    /**
     * Top-P (核采样) [0.0, 1.0]
     */
    @JsonProperty("top_p")
    private Float topP;
    
    // ========== Qwen-Specific Fields ==========
    
    /**
     * 随机种子，用于生成可复现的结果
     */
    @JsonProperty("seed")
    private Integer seed;
    
    /**
     * 用户标识符
     */
    @JsonProperty("user")
    private String user;
    
    /**
     * 响应格式
     */
    @JsonProperty("response_format")
    private ResponseFormat responseFormat;
    
    /**
     * 生成结果的数量
     */
    @JsonProperty("n")
    private Integer n;
    
    /**
     * 是否启用搜索增强
     */
    @JsonProperty("enable_search")
    private Boolean enableSearch;
    
    // ========== Response Format ==========
    
    /**
     * ResponseFormat - 响应格式配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ResponseFormat {
        /**
         * 格式类型: "text" 或 "json_object"
         */
        @JsonProperty("type")
        private String type;
        
        /**
         * 创建文本格式响应
         */
        public static ResponseFormat text() {
            return ResponseFormat.builder().type("text").build();
        }
        
        /**
         * 创建JSON对象格式响应
         */
        public static ResponseFormat jsonObject() {
            return ResponseFormat.builder().type("json_object").build();
        }
    }
    
    // ========== ChatOptions Interface Implementation ==========
    
    @Override
    public String getModel() {
        return model;
    }
    
    @Override
    public Float getFrequencyPenalty() {
        return frequencyPenalty;
    }
    
    @Override
    public Integer getMaxTokens() {
        return maxTokens;
    }
    
    @Override
    public Float getPresencePenalty() {
        return presencePenalty;
    }
    
    @Override
    public List<String> getStopSequences() {
        return stopSequences;
    }
    
    @Override
    public Float getTemperature() {
        return temperature;
    }
    
    @Override
    public Integer getTopK() {
        return topK;
    }
    
    @Override
    public Float getTopP() {
        return topP;
    }
    
    @Override
    public ChatOptions copy() {
        return this.toBuilder().build();
    }
    
    @Override
    public ChatOptions merge(ChatOptions other) {
        if (other == null) {
            return this;
        }
        
        QwenChatOptions.QwenChatOptionsBuilder builder = this.toBuilder();
        
        // Merge portable fields
        if (other.getModel() != null) {
            builder.model(other.getModel());
        }
        if (other.getFrequencyPenalty() != null) {
            builder.frequencyPenalty(other.getFrequencyPenalty());
        }
        if (other.getMaxTokens() != null) {
            builder.maxTokens(other.getMaxTokens());
        }
        if (other.getPresencePenalty() != null) {
            builder.presencePenalty(other.getPresencePenalty());
        }
        if (other.getStopSequences() != null) {
            builder.stopSequences(other.getStopSequences());
        }
        if (other.getTemperature() != null) {
            builder.temperature(other.getTemperature());
        }
        if (other.getTopK() != null) {
            builder.topK(other.getTopK());
        }
        if (other.getTopP() != null) {
            builder.topP(other.getTopP());
        }
        
        // Merge Qwen-specific fields if other is also QwenChatOptions
        if (other instanceof QwenChatOptions) {
            QwenChatOptions qwenOther = (QwenChatOptions) other;
            if (qwenOther.getSeed() != null) {
                builder.seed(qwenOther.getSeed());
            }
            if (qwenOther.getUser() != null) {
                builder.user(qwenOther.getUser());
            }
            if (qwenOther.getResponseFormat() != null) {
                builder.responseFormat(qwenOther.getResponseFormat());
            }
            if (qwenOther.getN() != null) {
                builder.n(qwenOther.getN());
            }
            if (qwenOther.getEnableSearch() != null) {
                builder.enableSearch(qwenOther.getEnableSearch());
            }
        }
        
        return builder.build();
    }
    
    // ========== Convenience Factory Methods ==========
    
    /**
     * 创建 qwen-turbo 默认配置
     */
    public static QwenChatOptionsBuilder qwenTurbo() {
        return QwenChatOptions.builder()
                .model("qwen-turbo")
                .temperature(0.7f)
                .maxTokens(1500);
    }
    
    /**
     * 创建 qwen-plus 默认配置
     */
    public static QwenChatOptionsBuilder qwenPlus() {
        return QwenChatOptions.builder()
                .model("qwen-plus")
                .temperature(0.7f)
                .maxTokens(2000);
    }
    
    /**
     * 创建 qwen-max 默认配置
     */
    public static QwenChatOptionsBuilder qwenMax() {
        return QwenChatOptions.builder()
                .model("qwen-max")
                .temperature(0.7f)
                .maxTokens(2000);
    }
    
    /**
     * 创建默认配置 (qwen-turbo)
     */
    public static QwenChatOptionsBuilder defaults() {
        return qwenTurbo();
    }
}
