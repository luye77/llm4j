package com.bobo.llm4j.chat;

import com.bobo.llm4j.base.BaseTest;
import com.bobo.llm4j.listener.StreamingResponseHandler;
import com.bobo.llm4j.platform.openai.chat.entity.Message;
import com.bobo.llm4j.platform.openai.chat.entity.Prompt;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import org.junit.Test;

/**
 * 聊天模型流式输出测试类
 * <p>
 * 测试聊天模型的流式输出功能，包括：
 * <ul>
 *     <li>基础流式输出</li>
 *     <li>思考模式流式输出</li>
 *     <li>流式输出错误处理</li>
 * </ul>
 * </p>
 *
 * @author bobo
 * @since 1.4.3
 */
@Slf4j
public class ChatModelStreamTest extends BaseTest {

    /**
     * 测试基础流式输出
     *
     * @throws Exception 调用异常
     */
    @Test
    public void testBasicStream() throws Exception {
        log.info("=== 测试基础流式输出 ===");

        Prompt prompt = Prompt.builder()
                .model(CHAT_MODEL)
                .message(Message.withUser("请用100字左右介绍人工智能"))
                .build();

        StreamingResponseHandler handler = new StreamingResponseHandler() {
            @Override
            protected void send() {
                String chunk = this.getCurrStr();
                if (chunk != null && !chunk.isEmpty()) {
                    System.out.print(chunk);
                }
            }

            @Override
            protected void error(Throwable t, Response response) {
                log.error("流式输出错误", t);
            }
        };

        long startTime = System.currentTimeMillis();
        log.info("开始流式请求...");

        chatModel.stream(prompt, handler);

        long endTime = System.currentTimeMillis();

        System.out.println();
        log.info("流式输出完成，耗时: {}ms", endTime - startTime);
        log.info("完整输出: {}", handler.getOutput().toString());
        log.info("Token使用: {}", handler.getUsage());
    }

    /**
     * 测试思考模式流式输出
     * <p>
     * 适用于支持reasoning的模型，如DeepSeek
     * </p>
     *
     * @throws Exception 调用异常
     */
    @Test
    public void testReasoningStream() throws Exception {
        log.info("=== 测试思考模式流式输出 ===");

        Prompt prompt = Prompt.builder()
                .model(CHAT_MODEL)
                .message(Message.withUser("9.11和9.8哪个大？请仔细思考"))
                .build();

        StreamingResponseHandler handler = new StreamingResponseHandler() {
            @Override
            protected void send() {
                String chunk = this.getCurrStr();
                if (chunk != null && !chunk.isEmpty()) {
                    if (this.isReasoning()) {
                        System.out.print("[思考] " + chunk);
                    } else {
                        System.out.print(chunk);
                    }
                }
            }
        };

        chatModel.stream(prompt, handler);

        System.out.println();
        log.info("思考内容: {}", handler.getReasoningOutput().toString());
        log.info("最终回答: {}", handler.getOutput().toString());
    }

    /**
     * 测试流式输出错误处理
     *
     * @throws Exception 调用异常
     */
    @Test
    public void testStreamErrorHandling() throws Exception {
        log.info("=== 测试流式输出错误处理 ===");

        Prompt prompt = Prompt.builder()
                .model(CHAT_MODEL)
                .message(Message.withUser("测试"))
                .build();

        StreamingResponseHandler handler = new StreamingResponseHandler() {
            @Override
            protected void send() {
                log.debug("收到: {}", this.getCurrStr());
            }

            @Override
            protected void error(Throwable t, Response response) {
                log.error("发生错误: {}", t != null ? t.getMessage() : "Unknown error");
                if (response != null) {
                    log.error("响应状态: {} {}", response.code(), response.message());
                }
            }
        };

        try {
            chatModel.stream(prompt, handler);
            log.info("请求完成，输出: {}", handler.getOutput().toString());
        } catch (Exception e) {
            log.error("捕获异常: {}", e.getMessage());
        }
    }

    /**
     * 测试长文本流式输出
     *
     * @throws Exception 调用异常
     */
    @Test
    public void testLongTextStream() throws Exception {
        log.info("=== 测试长文本流式输出 ===");

        Prompt prompt = Prompt.builder()
                .model(CHAT_MODEL)
                .message(Message.withUser("请详细介绍Java语言的发展历史"))
                .maxCompletionTokens(500)
                .build();

        StringBuilder fullOutput = new StringBuilder();

        StreamingResponseHandler handler = new StreamingResponseHandler() {
            @Override
            protected void send() {
                String chunk = this.getCurrStr();
                if (chunk != null && !chunk.isEmpty()) {
                    fullOutput.append(chunk);
                    System.out.print(chunk);
                }
            }
        };

        chatModel.stream(prompt, handler);

        System.out.println();
        log.info("输出字符数: {}", fullOutput.length());
        log.info("Token使用: {}", handler.getUsage());
    }
}
