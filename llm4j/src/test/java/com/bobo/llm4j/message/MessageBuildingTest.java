package com.bobo.llm4j.message;

import com.bobo.llm4j.chat.entity.Media;
import com.bobo.llm4j.chat.entity.Message;
import com.bobo.llm4j.chat.entity.Prompt;
import com.bobo.llm4j.enums.MessageType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * 消息构建测试类
 * <p>
 * 测试消息和媒体实体的构建功能，包括：
 * <ul>
 *     <li>各类型消息构建</li>
 *     <li>多模态消息构建</li>
 *     <li>Media实体构建</li>
 *     <li>Prompt构建</li>
 * </ul>
 * </p>
 *
 * @author bobo
 * @since 1.4.3
 */
@Slf4j
public class MessageBuildingTest {

    /**
     * 测试系统消息构建
     */
    @Test
    public void testSystemMessageBuilding() {
        log.info("=== 测试系统消息构建 ===");

        String content = "你是一个有帮助的助手";
        Message message = Message.withSystem(content);

        Assert.assertNotNull("消息不应为空", message);
        Assert.assertEquals("角色验证", MessageType.SYSTEM.getRole(), message.getRole());
        Assert.assertEquals("内容验证", content, message.getContent().getText());

        log.info("系统消息: role={}, content={}", message.getRole(), message.getContent().getText());
    }

    /**
     * 测试用户消息构建
     */
    @Test
    public void testUserMessageBuilding() {
        log.info("=== 测试用户消息构建 ===");

        String content = "你好，请介绍一下自己";
        Message message = Message.withUser(content);

        Assert.assertNotNull("消息不应为空", message);
        Assert.assertEquals("角色验证", MessageType.USER.getRole(), message.getRole());
        Assert.assertEquals("内容验证", content, message.getContent().getText());

        log.info("用户消息: role={}, content={}", message.getRole(), message.getContent().getText());
    }

    /**
     * 测试助手消息构建
     */
    @Test
    public void testAssistantMessageBuilding() {
        log.info("=== 测试助手消息构建 ===");

        String content = "你好！有什么可以帮助你的吗？";
        Message message = Message.withAssistant(content);

        Assert.assertNotNull("消息不应为空", message);
        Assert.assertEquals("角色验证", MessageType.ASSISTANT.getRole(), message.getRole());
        Assert.assertEquals("内容验证", content, message.getContent().getText());

        log.info("助手消息: role={}, content={}", message.getRole(), message.getContent().getText());
    }

    /**
     * 测试多模态用户消息构建
     */
    @Test
    public void testMultimodalUserMessageBuilding() {
        log.info("=== 测试多模态用户消息构建 ===");

        String content = "这是什么图片？";
        String imageUrl = "https://example.com/image.jpg";

        Message message = Message.withUser(content, imageUrl);

        Assert.assertNotNull("消息不应为空", message);
        Assert.assertEquals("角色验证", MessageType.USER.getRole(), message.getRole());
        Assert.assertNotNull("多模态内容不应为空", message.getContent().getMultiModals());
        Assert.assertEquals("多模态部件数量验证", 2, message.getContent().getMultiModals().size());

        log.info("多模态消息: role={}, multimodals={}",
                message.getRole(), message.getContent().getMultiModals().size());
    }

    /**
     * 测试多图片用户消息构建
     */
    @Test
    public void testMultipleImagesMessageBuilding() {
        log.info("=== 测试多图片用户消息构建 ===");

        String content = "比较这些图片";
        String imageUrl1 = "https://example.com/image1.jpg";
        String imageUrl2 = "https://example.com/image2.jpg";
        String imageUrl3 = "https://example.com/image3.jpg";

        Message message = Message.withUser(content, imageUrl1, imageUrl2, imageUrl3);

        Assert.assertNotNull("消息不应为空", message);
        Assert.assertEquals("多模态部件数量验证", 4, message.getContent().getMultiModals().size());

        log.info("多图片消息: multimodals={}", message.getContent().getMultiModals().size());
    }

    /**
     * 测试Media纯文本构建
     */
    @Test
    public void testMediaTextBuilding() throws Exception {
        log.info("=== 测试Media纯文本构建 ===");

        String text = "这是一段纯文本";
        Media media = Media.ofText(text);

        Assert.assertNotNull("Media不应为空", media);
        Assert.assertEquals("文本内容验证", text, media.getText());
        Assert.assertNull("多模态内容应为空", media.getMultiModals());

        ObjectMapper mapper = new ObjectMapper();
        log.info("纯文本JSON: {}", mapper.writeValueAsString(media));
    }

    /**
     * 测试Media多模态构建
     */
    @Test
    public void testMediaMultimodalBuilding() throws Exception {
        log.info("=== 测试Media多模态构建 ===");

        List<Media.MultiModal> multiModals = Media.MultiModal.withMultiModal(
                "请描述这些图片",
                "https://example.com/image1.jpg",
                "https://example.com/image2.jpg"
        );

        Media media = Media.ofMultiModals(multiModals);

        Assert.assertNotNull("Media不应为空", media);
        Assert.assertNull("文本内容应为空", media.getText());
        Assert.assertNotNull("多模态内容不应为空", media.getMultiModals());
        Assert.assertEquals("多模态部件数量验证", 3, media.getMultiModals().size());

        ObjectMapper mapper = new ObjectMapper();
        log.info("多模态JSON: {}", mapper.writeValueAsString(media));
    }

    /**
     * 测试MultiModal类型验证
     */
    @Test
    public void testMultiModalTypes() {
        log.info("=== 测试MultiModal类型验证 ===");

        List<Media.MultiModal> multiModals = Media.MultiModal.withMultiModal(
                "文本内容",
                "https://example.com/image.jpg"
        );

        Assert.assertEquals("第一个部件应为文本类型",
                Media.MultiModal.Type.TEXT.getType(), multiModals.get(0).getType());
        Assert.assertEquals("第二个部件应为图片类型",
                Media.MultiModal.Type.IMAGE_URL.getType(), multiModals.get(1).getType());

        log.info("文本类型: {}", multiModals.get(0).getType());
        log.info("图片类型: {}", multiModals.get(1).getType());
    }

    /**
     * 测试Prompt构建
     */
    @Test
    public void testPromptBuilding() throws Exception {
        log.info("=== 测试Prompt构建 ===");

        Prompt prompt = Prompt.builder()
                .model("gpt-4o-mini")
                .message(Message.withSystem("你是一个助手"))
                .message(Message.withUser("你好"))
                .temperature(0.7f)
                .topP(0.9f)
                .maxCompletionTokens(100)
                .build();

        Assert.assertNotNull("Prompt不应为空", prompt);
        Assert.assertEquals("模型验证", "gpt-4o-mini", prompt.getModel());
        Assert.assertEquals("消息数量验证", 2, prompt.getMessages().size());
        Assert.assertEquals("温度验证", 0.7f, prompt.getTemperature(), 0.01);

        ObjectMapper mapper = new ObjectMapper();
        log.info("Prompt JSON: {}", mapper.writeValueAsString(prompt));
    }

    /**
     * 测试Prompt默认值
     */
    @Test
    public void testPromptDefaultValues() {
        log.info("=== 测试Prompt默认值 ===");

        Prompt prompt = Prompt.builder()
                .model("gpt-4o-mini")
                .message(Message.withUser("测试"))
                .build();

        Assert.assertEquals("stream默认值验证", Boolean.FALSE, prompt.getStream());
        Assert.assertEquals("temperature默认值验证", 1f, prompt.getTemperature(), 0.01);
        Assert.assertEquals("topP默认值验证", 1f, prompt.getTopP(), 0.01);
        Assert.assertEquals("frequencyPenalty默认值验证", 0f, prompt.getFrequencyPenalty(), 0.01);
        Assert.assertEquals("presencePenalty默认值验证", 0f, prompt.getPresencePenalty(), 0.01);
        Assert.assertEquals("n默认值验证", Integer.valueOf(1), prompt.getN());

        log.info("默认值验证通过");
    }
}
