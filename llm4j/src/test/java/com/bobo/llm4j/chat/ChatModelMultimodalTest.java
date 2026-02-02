package com.bobo.llm4j.chat;

import com.bobo.llm4j.base.BaseTest;
import com.bobo.llm4j.platform.openai.chat.entity.ChatResponse;
import com.bobo.llm4j.platform.openai.chat.entity.Message;
import com.bobo.llm4j.platform.openai.chat.entity.Prompt;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

/**
 * 聊天模型多模态测试类
 * <p>
 * 测试聊天模型的多模态功能，包括：
 * <ul>
 *     <li>单张图片对话</li>
 *     <li>多张图片对话</li>
 *     <li>图片描述与分析</li>
 * </ul>
 * </p>
 *
 * @author bobo
 * @since 1.4.3
 */
@Slf4j
public class ChatModelMultimodalTest extends BaseTest {

    /**
     * 示例图片URL - 猫
     */
    private static final String CAT_IMAGE_URL =
            "https://upload.wikimedia.org/wikipedia/commons/thumb/3/3a/Cat03.jpg/1200px-Cat03.jpg";

    /**
     * 示例图片URL - 狗
     */
    private static final String DOG_IMAGE_URL =
            "https://upload.wikimedia.org/wikipedia/commons/thumb/2/26/" +
            "YellowLabradorLooking_new.jpg/1200px-YellowLabradorLooking_new.jpg";

    /**
     * 测试单张图片对话
     *
     * @throws Exception 调用异常
     */
    @Test
    public void testSingleImageChat() throws Exception {
        log.info("=== 测试单张图片对话 ===");

        Prompt prompt = Prompt.builder()
                .model(CHAT_MODEL)
                .message(Message.withUser("请描述这张图片中有什么？", CAT_IMAGE_URL))
                .build();

        log.info("多模态请求 - 包含图片URL");

        ObjectMapper mapper = new ObjectMapper();
        log.info("请求JSON: {}", mapper.writeValueAsString(prompt));

        ChatResponse response = chatModel.call(prompt);

        String content = response.getGenerations().get(0).getMessage().getContent().getText();
        log.info("助手回复: {}", content);
    }

    /**
     * 测试多张图片对话
     *
     * @throws Exception 调用异常
     */
    @Test
    public void testMultipleImagesChat() throws Exception {
        log.info("=== 测试多张图片对话 ===");

        Prompt prompt = Prompt.builder()
                .model(CHAT_MODEL)
                .message(Message.withUser(
                        "这些图片分别是什么动物？请分别描述。",
                        CAT_IMAGE_URL,
                        DOG_IMAGE_URL
                ))
                .build();

        ChatResponse response = chatModel.call(prompt);

        String content = response.getGenerations().get(0).getMessage().getContent().getText();
        log.info("助手回复: {}", content);
    }

    /**
     * 测试图片比较分析
     *
     * @throws Exception 调用异常
     */
    @Test
    public void testImageComparison() throws Exception {
        log.info("=== 测试图片比较分析 ===");

        Prompt prompt = Prompt.builder()
                .model(CHAT_MODEL)
                .message(Message.withSystem("你是一位动物专家，擅长分析和比较动物图片。"))
                .message(Message.withUser(
                        "请比较这两张图片中的动物，说明它们的相同点和不同点。",
                        CAT_IMAGE_URL,
                        DOG_IMAGE_URL
                ))
                .build();

        ChatResponse response = chatModel.call(prompt);

        String content = response.getGenerations().get(0).getMessage().getContent().getText();
        log.info("助手回复: {}", content);
    }

    /**
     * 测试图片细节描述
     *
     * @throws Exception 调用异常
     */
    @Test
    public void testImageDetailDescription() throws Exception {
        log.info("=== 测试图片细节描述 ===");

        Prompt prompt = Prompt.builder()
                .model(CHAT_MODEL)
                .message(Message.withUser(
                        "请详细描述这张图片，包括主体、背景、颜色、光线等细节。",
                        CAT_IMAGE_URL
                ))
                .maxCompletionTokens(300)
                .build();

        ChatResponse response = chatModel.call(prompt);

        String content = response.getGenerations().get(0).getMessage().getContent().getText();
        log.info("助手回复: {}", content);
    }
}
