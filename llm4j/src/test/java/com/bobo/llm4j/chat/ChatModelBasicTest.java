package com.bobo.llm4j.chat;

import com.bobo.llm4j.base.BaseTest;
import com.bobo.llm4j.platform.openai.chat.entity.ChatResponse;
import com.bobo.llm4j.platform.openai.chat.entity.Message;
import com.bobo.llm4j.platform.openai.chat.entity.Prompt;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 聊天模型基础测试类
 * <p>
 * 测试聊天模型的基础功能，包括：
 * <ul>
 *     <li>基础对话</li>
 *     <li>系统消息</li>
 *     <li>参数配置</li>
 *     <li>多轮对话</li>
 *     <li>响应格式控制</li>
 * </ul>
 * </p>
 *
 * @author bobo
 * @since 1.4.3
 */
@Slf4j
public class ChatModelBasicTest extends BaseTest {

    /**
     * 测试基础对话
     *
     * @throws Exception 调用异常
     */
    @Test
    public void testBasicChat() throws Exception {
        log.info("=== 测试基础对话 ===");

        Prompt prompt = Prompt.builder()
                .model(CHAT_MODEL)
                .message(Message.withUser("请用一句话介绍你自己"))
                .build();

        log.info("请求模型: {}", prompt.getModel());

        ChatResponse response = chatModel.call(prompt);

        String content = response.getGenerations().get(0).getMessage().getContent().getText();
        log.info("助手回复: {}", content);
        log.info("Token使用: {}", response.getUsage());
    }

    /**
     * 测试系统消息
     *
     * @throws Exception 调用异常
     */
    @Test
    public void testSystemMessage() throws Exception {
        log.info("=== 测试系统消息 ===");

        Prompt prompt = Prompt.builder()
                .model(CHAT_MODEL)
                .message(Message.withSystem("你是一位专业的Java开发工程师，回答问题时要专业、简洁。"))
                .message(Message.withUser("什么是单例模式？"))
                .build();

        ChatResponse response = chatModel.call(prompt);

        String content = response.getGenerations().get(0).getMessage().getContent().getText();
        log.info("助手回复: {}", content);
    }

    /**
     * 测试Prompt参数配置
     *
     * @throws Exception 调用异常
     */
    @Test
    public void testPromptParameters() throws Exception {
        log.info("=== 测试Prompt参数配置 ===");

        Prompt prompt = Prompt.builder()
                .model(CHAT_MODEL)
                .message(Message.withUser("讲一个简短的笑话"))
                .temperature(0.8f)
                .topP(0.9f)
                .maxCompletionTokens(100)
                .frequencyPenalty(0.5f)
                .presencePenalty(0.5f)
                .n(1)
                .build();

        log.info("参数配置 - temperature: {}, topP: {}, maxTokens: {}",
                prompt.getTemperature(), prompt.getTopP(), prompt.getMaxCompletionTokens());

        ChatResponse response = chatModel.call(prompt);

        String content = response.getGenerations().get(0).getMessage().getContent().getText();
        log.info("助手回复: {}", content);
    }

    /**
     * 测试多轮对话
     *
     * @throws Exception 调用异常
     */
    @Test
    public void testMultiTurnChat() throws Exception {
        log.info("=== 测试多轮对话 ===");

        List<Message> history = new ArrayList<>();

        // 第一轮对话
        history.add(Message.withUser("我叫小明，今年25岁"));

        Prompt firstPrompt = Prompt.builder()
                .model(CHAT_MODEL)
                .messages(history)
                .build();

        ChatResponse firstResponse = chatModel.call(firstPrompt);
        Message firstAssistant = firstResponse.getGenerations().get(0).getMessage();
        history.add(firstAssistant);

        log.info("第一轮 - 用户: 我叫小明，今年25岁");
        log.info("第一轮 - 助手: {}", firstAssistant.getContent().getText());

        // 第二轮对话
        history.add(Message.withUser("我刚才说我叫什么名字？多大了？"));

        Prompt secondPrompt = Prompt.builder()
                .model(CHAT_MODEL)
                .messages(history)
                .build();

        ChatResponse secondResponse = chatModel.call(secondPrompt);
        Message secondAssistant = secondResponse.getGenerations().get(0).getMessage();

        log.info("第二轮 - 用户: 我刚才说我叫什么名字？多大了？");
        log.info("第二轮 - 助手: {}", secondAssistant.getContent().getText());
        log.info("对话历史消息数: {}", history.size());
    }

    /**
     * 测试JSON响应格式
     *
     * @throws Exception 调用异常
     */
    @Test
    public void testJsonResponseFormat() throws Exception {
        log.info("=== 测试JSON响应格式 ===");

        Map<String, String> jsonFormat = new HashMap<>();
        jsonFormat.put("type", "json_object");

        Prompt prompt = Prompt.builder()
                .model(CHAT_MODEL)
                .message(Message.withSystem("你必须以JSON格式回复"))
                .message(Message.withUser("列出3种编程语言及其特点"))
                .responseFormat(jsonFormat)
                .build();

        ChatResponse response = chatModel.call(prompt);

        String content = response.getGenerations().get(0).getMessage().getContent().getText();
        log.info("JSON格式响应: {}", content);
    }

    /**
     * 测试停止词功能
     *
     * @throws Exception 调用异常
     */
    @Test
    public void testStopSequences() throws Exception {
        log.info("=== 测试停止词功能 ===");

        Prompt prompt = Prompt.builder()
                .model(CHAT_MODEL)
                .message(Message.withUser("从1数到10"))
                .stop(Arrays.asList("5", "五"))
                .build();

        ChatResponse response = chatModel.call(prompt);

        String content = response.getGenerations().get(0).getMessage().getContent().getText();
        log.info("响应（遇到停止词截断）: {}", content);
    }

    /**
     * 测试自定义URL和Key调用
     *
     * @throws Exception 调用异常
     */
    @Test
    public void testCustomUrlAndKey() throws Exception {
        log.info("=== 测试自定义URL和Key调用 ===");

        Prompt prompt = Prompt.builder()
                .model(CHAT_MODEL)
                .message(Message.withUser("你好"))
                .build();

        // 使用null表示使用默认配置
        ChatResponse response = chatModel.call(null, null, prompt);

        if (response != null) {
            String content = response.getGenerations().get(0).getMessage().getContent().getText();
            log.info("响应: {}", content);
        }
    }
}
