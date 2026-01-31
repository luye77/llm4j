package com.bobo.llm4j.platform.openai.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.bobo.llm4j.config.OpenAiConfig;
import com.bobo.llm4j.constant.Constants;
import com.bobo.llm4j.listener.StreamingResponseHandler;
import com.bobo.llm4j.platform.openai.chat.entity.*;
import com.bobo.llm4j.platform.openai.tool.ToolDefinition;
import com.bobo.llm4j.platform.openai.tool.ToolCall;
import com.bobo.llm4j.service.Configuration;
import com.bobo.llm4j.service.ChatModel;
import com.bobo.llm4j.utils.ToolUtil;
import com.bobo.llm4j.utils.ValidateUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.sse.EventSource;

import java.util.ArrayList;
import java.util.List;

/**
 * OpenAiChatModel - OpenAI Chat模型实现 (对应Spring AI的OpenAiChatModel)
 */
@Slf4j
public class OpenAiChatModel implements ChatModel {

    private final OpenAiConfig openAiConfig;
    private final OkHttpClient okHttpClient;
    private final EventSource.Factory factory;

    public OpenAiChatModel(Configuration configuration) {
        this.openAiConfig = configuration.getOpenAiConfig();
        this.okHttpClient = configuration.getOkHttpClient();
        this.factory = configuration.createRequestFactory();
    }

    public OpenAiChatModel(Configuration configuration, OpenAiConfig openAiConfig) {
        this.openAiConfig = openAiConfig;
        this.okHttpClient = configuration.getOkHttpClient();
        this.factory = configuration.createRequestFactory();
    }

    @Override
    public ChatResponse call(String baseUrl, String apiKey, Prompt prompt) throws Exception {
        if(baseUrl == null || "".equals(baseUrl)) baseUrl = openAiConfig.getApiHost();
        if(apiKey == null || "".equals(apiKey)) apiKey = openAiConfig.getApiKey();
        prompt.setStream(false);
        prompt.setStreamOptions(null);

        if(prompt.getFunctions()!=null && !prompt.getFunctions().isEmpty()){
            List<ToolDefinition> tools = ToolUtil.getAllTools(prompt.getFunctions());
            prompt.setTools(tools);
            if(tools == null){
                prompt.setParallelToolCalls(null);
            }
        }
        if (prompt.getTools()!=null && !prompt.getTools().isEmpty()){
            // has tools
        }else{
            prompt.setParallelToolCalls(null);
        }

        Usage allUsage = new Usage();
        String finishReason = "first";

        while("first".equals(finishReason) || "tool_calls".equals(finishReason)){
            finishReason = null;

            ObjectMapper mapper = new ObjectMapper();
            String requestString = mapper.writeValueAsString(prompt);

            Request request = new Request.Builder()
                    .header("Authorization", "Bearer " + apiKey)
                    .url(ValidateUtil.concatUrl(baseUrl, openAiConfig.getChatCompletionUrl()))
                    .post(RequestBody.create(MediaType.parse(Constants.JSON_CONTENT_TYPE), requestString))
                    .build();

            Response execute = okHttpClient.newCall(request).execute();
            if (execute.isSuccessful() && execute.body() != null){
                ChatResponse chatResponse = mapper.readValue(execute.body().string(), ChatResponse.class);

                Generation generation = chatResponse.getGenerations().get(0);
                finishReason = generation.getFinishReason();

                Usage usage = chatResponse.getUsage();
                allUsage.setCompletionTokens(allUsage.getCompletionTokens() + usage.getCompletionTokens());
                allUsage.setTotalTokens(allUsage.getTotalTokens() + usage.getTotalTokens());
                allUsage.setPromptTokens(allUsage.getPromptTokens() + usage.getPromptTokens());

                if("tool_calls".equals(finishReason)){
                    Message message = generation.getMessage();
                    List<ToolCall> toolCalls = message.getToolCalls();

                    List<Message> messages = new ArrayList<>(prompt.getMessages());
                    messages.add(message);

                    for (ToolCall toolCall : toolCalls) {
                        String functionName = toolCall.getFunction().getName();
                        String arguments = toolCall.getFunction().getArguments();
                        String functionResponse = ToolUtil.invoke(functionName, arguments);

                        messages.add(Message.withTool(functionResponse, toolCall.getId()));
                    }
                    prompt.setMessages(messages);
                }else{
                    chatResponse.setUsage(allUsage);
                    return chatResponse;
                }
            }else{
                return null;
            }
        }

        return null;
    }

    @Override
    public ChatResponse call(Prompt prompt) throws Exception {
        return call(null, null, prompt);
    }

    @Override
    public void stream(String baseUrl, String apiKey, Prompt prompt, StreamingResponseHandler handler) throws Exception {
        if(baseUrl == null || "".equals(baseUrl)) baseUrl = openAiConfig.getApiHost();
        if(apiKey == null || "".equals(apiKey)) apiKey = openAiConfig.getApiKey();
        prompt.setStream(true);
        StreamOptions streamOptions = prompt.getStreamOptions();
        if(streamOptions == null){
            prompt.setStreamOptions(new StreamOptions(true));
        }

        if(prompt.getFunctions()!=null && !prompt.getFunctions().isEmpty()){
            List<ToolDefinition> tools = ToolUtil.getAllTools(prompt.getFunctions());
            prompt.setTools(tools);
            if(tools == null){
                prompt.setParallelToolCalls(null);
            }
        }

        if (prompt.getTools()!=null && !prompt.getTools().isEmpty()){
            // has tools
        }else{
            prompt.setParallelToolCalls(null);
        }

        String finishReason = "first";

        while("first".equals(finishReason) || "tool_calls".equals(finishReason)){
            finishReason = null;
            ObjectMapper mapper = new ObjectMapper();
            String jsonString = mapper.writeValueAsString(prompt);

            Request request = new Request.Builder()
                    .header("Authorization", "Bearer " + apiKey)
                    .url(ValidateUtil.concatUrl(baseUrl, openAiConfig.getChatCompletionUrl()))
                    .post(RequestBody.create(MediaType.parse(Constants.APPLICATION_JSON), jsonString))
                    .build();

            factory.newEventSource(request, handler);
            handler.getCountDownLatch().await();

            finishReason = handler.getFinishReason();
            List<ToolCall> toolCalls = handler.getToolCalls();

            if("tool_calls".equals(finishReason) && !toolCalls.isEmpty()){
                Message responseMessage = Message.withAssistant(handler.getToolCalls());

                List<Message> messages = new ArrayList<>(prompt.getMessages());
                messages.add(responseMessage);

                for (ToolCall toolCall : toolCalls) {
                    String functionName = toolCall.getFunction().getName();
                    String arguments = toolCall.getFunction().getArguments();
                    String functionResponse = ToolUtil.invoke(functionName, arguments);

                    messages.add(Message.withTool(functionResponse, toolCall.getId()));
                }
                handler.setToolCalls(new ArrayList<>());
                handler.setToolCall(null);
                prompt.setMessages(messages);
            }
        }
    }

    @Override
    public void stream(Prompt prompt, StreamingResponseHandler handler) throws Exception {
        stream(null, null, prompt, handler);
    }
}
