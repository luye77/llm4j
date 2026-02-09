package com.bobo.llm4j.service;

import com.bobo.llm4j.http.StreamingResponseHandler;
import com.bobo.llm4j.platform.openai.chat.entity.Prompt;
import com.bobo.llm4j.platform.openai.chat.entity.ChatResponse;

/**
 * ChatModel - 聊天模型接口 (对应Spring AI的ChatModel)
 */
public interface ChatModel {

    ChatResponse call(String baseUrl, String apiKey, Prompt prompt) throws Exception;
    ChatResponse call(Prompt prompt) throws Exception;
    void stream(String baseUrl, String apiKey, Prompt prompt, StreamingResponseHandler handler) throws Exception;
    void stream(Prompt prompt, StreamingResponseHandler handler) throws Exception;

}
