package com.bobo.llm4j.config;

import com.bobo.llm4j.base.BaseTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;

/**
 * 配置管理测试类
 * <p>
 * 测试框架的配置管理功能，包括：
 * <ul>
 *     <li>OpenAiConfig配置验证</li>
 *     <li>Configuration组件注入验证</li>
 *     <li>AiService服务获取验证</li>
 * </ul>
 * </p>
 *
 * @author bobo
 * @since 1.4.3
 */
@Slf4j
public class ConfigurationTest extends BaseTest {

    /**
     * 测试OpenAI配置属性
     */
    @Test
    public void testOpenAiConfigProperties() {
        log.info("=== 测试OpenAI配置属性 ===");

        OpenAiConfig config = configuration.getOpenAiConfig();

        Assert.assertNotNull("配置对象不应为空", config);
        Assert.assertNotNull("API Host不应为空", config.getApiHost());
        Assert.assertNotNull("Chat Completion URL不应为空", config.getChatCompletionUrl());
        Assert.assertNotNull("Embedding URL不应为空", config.getEmbeddingUrl());

        log.info("API Host: {}", config.getApiHost());
        log.info("Chat Completion URL: {}", config.getChatCompletionUrl());
        log.info("Embedding URL: {}", config.getEmbeddingUrl());
    }

    /**
     * 测试Configuration组件注入
     */
    @Test
    public void testConfigurationComponents() {
        log.info("=== 测试Configuration组件注入 ===");

        Assert.assertNotNull("Configuration不应为空", configuration);
        Assert.assertNotNull("OkHttpClient不应为空", configuration.getOkHttpClient());
        Assert.assertNotNull("OpenAiConfig不应为空", configuration.getOpenAiConfig());

        log.info("Configuration组件注入验证通过");
    }

    /**
     * 测试自定义配置创建
     */
    @Test
    public void testCustomConfigCreation() {
        log.info("=== 测试自定义配置创建 ===");

        String customHost = "https://api.custom.com/";
        String customKey = "custom-key";
        String customChatUrl = "v2/chat";
        String customEmbedUrl = "v2/embed";

        OpenAiConfig customConfig = new OpenAiConfig();
        customConfig.setApiHost(customHost);
        customConfig.setApiKey(customKey);
        customConfig.setChatCompletionUrl(customChatUrl);
        customConfig.setEmbeddingUrl(customEmbedUrl);

        Assert.assertEquals("API Host设置验证", customHost, customConfig.getApiHost());
        Assert.assertEquals("API Key设置验证", customKey, customConfig.getApiKey());
        Assert.assertEquals("Chat URL设置验证", customChatUrl, customConfig.getChatCompletionUrl());
        Assert.assertEquals("Embed URL设置验证", customEmbedUrl, customConfig.getEmbeddingUrl());

        log.info("自定义配置创建验证通过");
    }
}
