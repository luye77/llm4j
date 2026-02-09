package com.bobo.llm4j.platform.qwen.chat;

import com.bobo.llm4j.config.QwenConfig;
import com.bobo.llm4j.constant.Constants;
import com.bobo.llm4j.http.StreamingResponseHandler;
import com.bobo.llm4j.platform.openai.chat.entity.ChatResponse;
import com.bobo.llm4j.platform.openai.chat.entity.Generation;
import com.bobo.llm4j.platform.openai.chat.entity.Media;
import com.bobo.llm4j.platform.openai.chat.entity.Message;
import com.bobo.llm4j.platform.openai.chat.entity.Prompt;
import com.bobo.llm4j.platform.openai.chat.entity.StreamOptions;
import com.bobo.llm4j.platform.openai.chat.entity.Usage;
import com.bobo.llm4j.platform.openai.tool.ToolCall;
import com.bobo.llm4j.platform.openai.tool.ToolDefinition;
import com.bobo.llm4j.platform.qwen.chat.entity.QwenChatRequest;
import com.bobo.llm4j.platform.qwen.chat.entity.QwenChatResponse;
import com.bobo.llm4j.service.ChatModel;
import com.bobo.llm4j.service.Configuration;
import com.bobo.llm4j.utils.ToolUtil;
import com.bobo.llm4j.utils.ValidateUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * QwenChatModel - 通义千问 Chat模型实现 (OpenAI兼容模式 / DashScope)
 */
@Slf4j
public class QwenChatModel implements ChatModel {

    private final QwenConfig qwenConfig;
    private final OkHttpClient okHttpClient;
    private final EventSource.Factory factory;

    public QwenChatModel(Configuration configuration) {
        this.qwenConfig = configuration.getQwenConfig();
        this.okHttpClient = configuration.getOkHttpClient();
        this.factory = configuration.createRequestFactory();
    }

    public QwenChatModel(Configuration configuration, QwenConfig qwenConfig) {
        this.qwenConfig = qwenConfig;
        this.okHttpClient = configuration.getOkHttpClient();
        this.factory = configuration.createRequestFactory();
    }

    @Override
    public ChatResponse call(String baseUrl, String apiKey, Prompt prompt) throws Exception {
        if (isCompatibleMode()) {
            return callCompatible(baseUrl, apiKey, prompt);
        }
        return callDashscope(baseUrl, apiKey, prompt);
    }

    @Override
    public ChatResponse call(Prompt prompt) throws Exception {
        return call(null, null, prompt);
    }

    @Override
    public void stream(String baseUrl, String apiKey, Prompt prompt, StreamingResponseHandler handler) throws Exception {
        if (isCompatibleMode()) {
            streamCompatible(baseUrl, apiKey, prompt, handler);
            return;
        }
        streamDashscope(baseUrl, apiKey, prompt, handler);
    }

    private boolean isCompatibleMode() {
        return qwenConfig == null || qwenConfig.isCompatibleMode();
    }

    private ChatResponse callCompatible(String baseUrl, String apiKey, Prompt prompt) throws Exception {
        if (baseUrl == null || "".equals(baseUrl)) baseUrl = qwenConfig.getApiHost();
        if (apiKey == null || "".equals(apiKey)) apiKey = qwenConfig.getApiKey();
        prompt.setStream(false);
        prompt.setStreamOptions(null);

        if (prompt.getFunctions() != null && !prompt.getFunctions().isEmpty()) {
            List<ToolDefinition> tools = ToolUtil.getAllTools(prompt.getFunctions());
            prompt.setTools(tools);
            if (tools == null) {
                prompt.setParallelToolCalls(null);
            }
        }
        if (prompt.getTools() != null && !prompt.getTools().isEmpty()) {
            // has tools
        } else {
            prompt.setParallelToolCalls(null);
        }

        Usage allUsage = new Usage();
        String finishReason = "first";

        while ("first".equals(finishReason) || "tool_calls".equals(finishReason)) {
            finishReason = null;

            ObjectMapper mapper = new ObjectMapper();
            String requestString = mapper.writeValueAsString(prompt);

            Request request = new Request.Builder()
                    .header("Authorization", "Bearer " + apiKey)
                    .url(ValidateUtil.concatUrl(baseUrl, qwenConfig.getChatCompletionUrl()))
                    .post(RequestBody.create(requestString, MediaType.parse(Constants.JSON_CONTENT_TYPE)))
                    .build();

            Response execute = okHttpClient.newCall(request).execute();
            if (execute.isSuccessful() && execute.body() != null) {
                ChatResponse chatResponse = mapper.readValue(execute.body().string(), ChatResponse.class);

                Generation generation = chatResponse.getGenerations().get(0);
                finishReason = generation.getFinishReason();

                Usage usage = chatResponse.getUsage();
                allUsage.setCompletionTokens(allUsage.getCompletionTokens() + usage.getCompletionTokens());
                allUsage.setTotalTokens(allUsage.getTotalTokens() + usage.getTotalTokens());
                allUsage.setPromptTokens(allUsage.getPromptTokens() + usage.getPromptTokens());

                if ("tool_calls".equals(finishReason)) {
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
                } else {
                    chatResponse.setUsage(allUsage);
                    return chatResponse;
                }
            } else {
                return null;
            }
        }

        return null;
    }

    private void streamCompatible(String baseUrl, String apiKey, Prompt prompt, StreamingResponseHandler handler) throws Exception {
        if (baseUrl == null || "".equals(baseUrl)) baseUrl = qwenConfig.getApiHost();
        if (apiKey == null || "".equals(apiKey)) apiKey = qwenConfig.getApiKey();
        prompt.setStream(true);
        StreamOptions streamOptions = prompt.getStreamOptions();
        if (streamOptions == null) {
            prompt.setStreamOptions(new StreamOptions(true));
        }

        if (prompt.getFunctions() != null && !prompt.getFunctions().isEmpty()) {
            List<ToolDefinition> tools = ToolUtil.getAllTools(prompt.getFunctions());
            prompt.setTools(tools);
            if (tools == null) {
                prompt.setParallelToolCalls(null);
            }
        }

        if (prompt.getTools() != null && !prompt.getTools().isEmpty()) {
            // has tools
        } else {
            prompt.setParallelToolCalls(null);
        }

        String finishReason = "first";

        while ("first".equals(finishReason) || "tool_calls".equals(finishReason)) {
            finishReason = null;
            ObjectMapper mapper = new ObjectMapper();
            String jsonString = mapper.writeValueAsString(prompt);

            Request request = new Request.Builder()
                    .header("Authorization", "Bearer " + apiKey)
                    .url(ValidateUtil.concatUrl(baseUrl, qwenConfig.getChatCompletionUrl()))
                    .post(RequestBody.create(jsonString, MediaType.parse(Constants.APPLICATION_JSON)))
                    .build();

            factory.newEventSource(request, handler);
            handler.getCountDownLatch().await();

            finishReason = handler.getFinishReason();
            List<ToolCall> toolCalls = handler.getToolCalls();

            if ("tool_calls".equals(finishReason) && !toolCalls.isEmpty()) {
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

    private ChatResponse callDashscope(String baseUrl, String apiKey, Prompt prompt) throws Exception {
        if (baseUrl == null || "".equals(baseUrl)) baseUrl = qwenConfig.getApiHost();
        if (apiKey == null || "".equals(apiKey)) apiKey = qwenConfig.getApiKey();

        QwenChatRequest chatRequest = buildDashscopeRequest(prompt, false);
        ObjectMapper mapper = new ObjectMapper();
        String requestString = mapper.writeValueAsString(chatRequest);

        Request request = new Request.Builder()
                .header("Authorization", "Bearer " + apiKey)
                .url(ValidateUtil.concatUrl(baseUrl, resolveDashscopeChatUrl(prompt)))
                .post(RequestBody.create(requestString, MediaType.parse(Constants.JSON_CONTENT_TYPE)))
                .build();

        Response execute = okHttpClient.newCall(request).execute();
        if (execute.isSuccessful() && execute.body() != null) {
            QwenChatResponse qwenChatResponse = mapper.readValue(execute.body().string(), QwenChatResponse.class);
            return toOpenAiChatResponse(qwenChatResponse, prompt.getModel(), false);
        }
        return null;
    }

    private void streamDashscope(String baseUrl, String apiKey, Prompt prompt, StreamingResponseHandler handler) throws Exception {
        if (baseUrl == null || "".equals(baseUrl)) baseUrl = qwenConfig.getApiHost();
        if (apiKey == null || "".equals(apiKey)) apiKey = qwenConfig.getApiKey();

        QwenChatRequest chatRequest = buildDashscopeRequest(prompt, true);
        ObjectMapper mapper = new ObjectMapper();
        String jsonString = mapper.writeValueAsString(chatRequest);

        Request request = new Request.Builder()
                .header("Authorization", "Bearer " + apiKey)
                .header("Accept", Constants.SSE_CONTENT_TYPE)
                .url(ValidateUtil.concatUrl(baseUrl, resolveDashscopeChatUrl(prompt)))
                .post(RequestBody.create(jsonString, MediaType.parse(Constants.APPLICATION_JSON)))
                .build();

        EventSourceListener listener = new EventSourceListener() {
            @Override
            public void onEvent(EventSource eventSource, String id, String type, String data) {
                if ("[DONE]".equalsIgnoreCase(data)) {
                    handler.onEvent(eventSource, id, type, data);
                    return;
                }
                try {
                    QwenChatResponse qwenChatResponse = mapper.readValue(data, QwenChatResponse.class);
                    ChatResponse openAiResponse = toOpenAiChatResponse(qwenChatResponse, prompt.getModel(), true);
                    String openAiJson = mapper.writeValueAsString(openAiResponse);
                    handler.onEvent(eventSource, id, type, openAiJson);
                } catch (Exception e) {
                    handler.onFailure(eventSource, e, null);
                }
            }

            @Override
            public void onFailure(EventSource eventSource, Throwable t, Response response) {
                handler.onFailure(eventSource, t, response);
            }

            @Override
            public void onClosed(EventSource eventSource) {
                handler.onClosed(eventSource);
            }
        };

        factory.newEventSource(request, listener);
        handler.getCountDownLatch().await();
    }

    @SuppressWarnings("deprecation")
    private QwenChatRequest buildDashscopeRequest(Prompt prompt, boolean stream) {
        QwenChatRequest request = new QwenChatRequest();
        request.setModel(prompt.getModel());
        request.setInput(new QwenChatRequest.Input(prompt.getMessages()));

        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("result_format", "message");
        if (stream) {
            parameters.put("incremental_output", true);
        }
        parameters.put("temperature", prompt.getTemperature());
        parameters.put("top_p", prompt.getTopP());
        Integer maxTokens = prompt.getMaxCompletionTokens() != null
                ? prompt.getMaxCompletionTokens()
                : prompt.getMaxTokens();
        if (maxTokens != null) {
            parameters.put("max_tokens", maxTokens);
        }
        if (prompt.getStop() != null && !prompt.getStop().isEmpty()) {
            parameters.put("stop", prompt.getStop());
        }
        parameters.put("presence_penalty", prompt.getPresencePenalty());
        if (prompt.getParameters() != null && !prompt.getParameters().isEmpty()) {
            parameters.putAll(prompt.getParameters());
        }
        if (!parameters.isEmpty()) {
            request.setParameters(parameters);
        }
        return request;
    }

    private String resolveDashscopeChatUrl(Prompt prompt) {
        if (hasImageContent(prompt)) {
            return qwenConfig.getDashscopeMultiModalCompletionUrl();
        }
        return qwenConfig.getDashscopeChatCompletionUrl();
    }

    private boolean hasImageContent(Prompt prompt) {
        if (prompt == null || prompt.getMessages() == null) {
            return false;
        }
        for (Message message : prompt.getMessages()) {
            Media content = message.getContent();
            if (content != null && content.getMultiModals() != null) {
                for (Media.MultiModal part : content.getMultiModals()) {
                    if (part.getImageUrl() != null
                            || Media.MultiModal.Type.IMAGE_URL.getType().equals(part.getType())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private ChatResponse toOpenAiChatResponse(QwenChatResponse qwenChatResponse, String model, boolean streaming) {
        ChatResponse response = new ChatResponse();
        if (qwenChatResponse == null) {
            return response;
        }
        response.setId(qwenChatResponse.getRequestId());
        response.setModel(model);
        response.setUsage(toOpenAiUsage(qwenChatResponse.getUsage()));

        List<Generation> generations = new ArrayList<>();
        if (qwenChatResponse.getOutput() != null && qwenChatResponse.getOutput().getChoices() != null) {
            int index = 0;
            for (QwenChatResponse.Choice choice : qwenChatResponse.getOutput().getChoices()) {
                Generation generation = new Generation();
                generation.setIndex(index++);
                generation.setFinishReason(choice.getFinishReason());
                if (streaming) {
                    Message delta = choice.getDelta() != null ? choice.getDelta() : choice.getMessage();
                    generation.setDelta(delta);
                } else {
                    Message message = choice.getMessage() != null ? choice.getMessage() : choice.getDelta();
                    generation.setMessage(message);
                }
                generations.add(generation);
            }
        }
        response.setGenerations(generations);
        return response;
    }

    private Usage toOpenAiUsage(QwenChatResponse.Usage usage) {
        if (usage == null) {
            return null;
        }
        Usage mapped = new Usage();
        mapped.setPromptTokens(usage.getInputTokens());
        mapped.setCompletionTokens(usage.getOutputTokens() == null ? 0 : usage.getOutputTokens());
        if (usage.getTotalTokens() != null) {
            mapped.setTotalTokens(usage.getTotalTokens());
        } else if (usage.getInputTokens() != null && usage.getOutputTokens() != null) {
            mapped.setTotalTokens(usage.getInputTokens() + usage.getOutputTokens());
        } else {
            mapped.setTotalTokens(usage.getInputTokens());
        }
        return mapped;
    }

    @Override
    public void stream(Prompt prompt, StreamingResponseHandler handler) throws Exception {
        stream(null, null, prompt, handler);
    }
}
