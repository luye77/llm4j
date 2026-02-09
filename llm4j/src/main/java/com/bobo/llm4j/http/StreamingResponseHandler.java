package com.bobo.llm4j.http;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.bobo.llm4j.exception.CommonException;
import com.bobo.llm4j.chat.entity.ChatResponse;
import com.bobo.llm4j.chat.entity.Message;
import com.bobo.llm4j.chat.entity.Generation;
import com.bobo.llm4j.chat.entity.Usage;
import com.bobo.llm4j.enums.MessageType;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * StreamingResponseHandler - 流式响应处理器 (对应Spring AI的StreamingChatResponseHandler)
 */
@Slf4j
public abstract class StreamingResponseHandler extends EventSourceListener {
    /**
     * 异常回调
     */
    protected void error(Throwable t, Response response) {}

    protected abstract void send();

    /**
     * 最终的消息输出
     */
    @Getter
    private final StringBuilder output = new StringBuilder();

    /**
     * 流式输出，当前消息的内容
     */
    @Getter
    private String currStr = "";

    /**
     * 流式输出，当前单条SSE消息对象
     */
    @Getter
    private String currData = "";

    /**
     * 记录当前是否为思考状态
     */
    @Getter
    private boolean isReasoning = false;

    /**
     * 思考内容的输出
     */
    @Getter
    private final StringBuilder reasoningOutput = new StringBuilder();

    /**
     * Token使用量
     */
    @Getter
    private final Usage usage = new Usage();

    @Getter
    private CountDownLatch countDownLatch = new CountDownLatch(1);

    @Getter
    @Setter
    private String finishReason = null;

    @Getter
    private EventSource eventSource = null;

    public boolean isAllFieldsNull(Object obj) throws IllegalAccessException {
        for (Field field : obj.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            if (field.get(obj) != null) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onFailure(@NotNull EventSource eventSource, @Nullable Throwable t, @Nullable Response response) {
        this.error(t, response);
        countDownLatch.countDown();
    }

    @Override
    public void onEvent(@NotNull EventSource eventSource, @Nullable String id, @Nullable String type, @NotNull String data) {
        currData = data;
        if(this.eventSource == null) {
            this.eventSource = eventSource;
        }

        if ("[DONE]".equalsIgnoreCase(data)) {
            currStr = "";
            this.send();
            return;
        }

        ObjectMapper objectMapper = new ObjectMapper();
        ChatResponse chatResponse = null;
        try {
            chatResponse = objectMapper.readValue(data, ChatResponse.class);
        } catch (JsonProcessingException e) {
            throw new CommonException("read data error");
        }

        // 统计token
        Usage currUsage = chatResponse.getUsage();
        if(currUsage != null){
            usage.setPromptTokens(usage.getPromptTokens() + currUsage.getPromptTokens());
            usage.setCompletionTokens(usage.getCompletionTokens() + currUsage.getCompletionTokens());
            usage.setTotalTokens(usage.getTotalTokens() + currUsage.getTotalTokens());
        }

        List<Generation> generations = chatResponse.getGenerations();

        if((generations == null || generations.isEmpty()) && chatResponse.getUsage() != null){
            this.currStr = "";
            this.send();
            return;
        }

        if(generations == null || generations.isEmpty()){
            return;
        }
        Message responseMessage = generations.get(0).getDelta();

        try {
            if ((isAllFieldsNull(responseMessage) || (responseMessage.getContent()!= null && StringUtils.isBlank(responseMessage.getContent().getText()))) && generations.get(0).getFinishReason() == null && !isAllFieldsNull(this.usage)) {
                this.currStr = "";
                this.send();
                return;
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        finishReason = generations.get(0).getFinishReason();

        if ("stop".equals(finishReason)) {
            if(responseMessage.getContent() != null && responseMessage.getContent().getText() != null) {
                currStr = responseMessage.getContent().getText();
                output.append(currStr);
            }else {
                currStr = "";
            }
            this.send();
            return;
        }

        if(MessageType.ASSISTANT.getRole().equals(responseMessage.getRole())
                && (responseMessage.getContent()==null || StringUtils.isEmpty(responseMessage.getContent().getText()))
                && StringUtils.isEmpty(responseMessage.getReasoningContent())){
            return;
        }

        if(StringUtils.isNotEmpty(responseMessage.getReasoningContent())){
            isReasoning = true;
            reasoningOutput.append(responseMessage.getReasoningContent());
            currStr = responseMessage.getReasoningContent();
        }else {
            isReasoning = false;
            if (responseMessage.getContent() == null) {
                this.send();
                return;
            }
            output.append(responseMessage.getContent().getText());
            currStr = responseMessage.getContent().getText();
        }

        this.send();
    }

    @Override
    public void onClosed(@NotNull EventSource eventSource) {
        countDownLatch.countDown();
        countDownLatch = new CountDownLatch(1);
    }
}

