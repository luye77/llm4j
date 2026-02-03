package com.bobo.llm4j.platform.qwen.embedding;

import com.alibaba.fastjson2.JSON;
import com.bobo.llm4j.config.QwenConfig;
import com.bobo.llm4j.constant.Constants;
import com.bobo.llm4j.platform.openai.chat.entity.Usage;
import com.bobo.llm4j.platform.openai.embedding.entity.Embedding;
import com.bobo.llm4j.platform.openai.embedding.entity.EmbeddingRequest;
import com.bobo.llm4j.platform.openai.embedding.entity.EmbeddingResponse;
import com.bobo.llm4j.platform.qwen.embedding.entity.QwenEmbeddingRequest;
import com.bobo.llm4j.platform.qwen.embedding.entity.QwenEmbeddingResponse;
import com.bobo.llm4j.service.Configuration;
import com.bobo.llm4j.service.EmbeddingModel;
import com.bobo.llm4j.utils.ValidateUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.util.ArrayList;
import java.util.List;

/**
 * QwenEmbeddingModel - 通义千问 Embedding模型实现 (OpenAI兼容模式 / DashScope)
 */
public class QwenEmbeddingModel implements EmbeddingModel {

    private final QwenConfig qwenConfig;
    private final OkHttpClient okHttpClient;

    public QwenEmbeddingModel(Configuration configuration) {
        this.qwenConfig = configuration.getQwenConfig();
        this.okHttpClient = configuration.getOkHttpClient();
    }

    public QwenEmbeddingModel(Configuration configuration, QwenConfig qwenConfig) {
        this.qwenConfig = qwenConfig;
        this.okHttpClient = configuration.getOkHttpClient();
    }

    @Override
    public EmbeddingResponse call(String baseUrl, String apiKey, EmbeddingRequest request) throws Exception {
        if (isCompatibleMode()) {
            return callCompatible(baseUrl, apiKey, request);
        }
        return callDashscope(baseUrl, apiKey, request);
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) throws Exception {
        return call(null, null, request);
    }

    private boolean isCompatibleMode() {
        return qwenConfig == null || qwenConfig.isCompatibleMode();
    }

    private EmbeddingResponse callCompatible(String baseUrl, String apiKey, EmbeddingRequest request) throws Exception {
        if (baseUrl == null || "".equals(baseUrl)) baseUrl = qwenConfig.getApiHost();
        if (apiKey == null || "".equals(apiKey)) apiKey = qwenConfig.getApiKey();
        String jsonString = JSON.toJSONString(request);

        Request httpRequest = new Request.Builder()
                .header("Authorization", "Bearer " + apiKey)
                .url(ValidateUtil.concatUrl(baseUrl, qwenConfig.getEmbeddingUrl()))
                .post(RequestBody.create(jsonString, MediaType.parse(Constants.APPLICATION_JSON)))
                .build();
        Response execute = okHttpClient.newCall(httpRequest).execute();
        if (execute.isSuccessful() && execute.body() != null) {
            return JSON.parseObject(execute.body().string(), EmbeddingResponse.class);
        }
        return null;
    }

    private EmbeddingResponse callDashscope(String baseUrl, String apiKey, EmbeddingRequest request) throws Exception {
        if (baseUrl == null || "".equals(baseUrl)) baseUrl = qwenConfig.getApiHost();
        if (apiKey == null || "".equals(apiKey)) apiKey = qwenConfig.getApiKey();

        QwenEmbeddingRequest embeddingRequest = new QwenEmbeddingRequest();
        embeddingRequest.setModel(request.getModel());
        embeddingRequest.setInput(new QwenEmbeddingRequest.Input(resolveInputTexts(request)));

        ObjectMapper mapper = new ObjectMapper();
        String jsonString = mapper.writeValueAsString(embeddingRequest);

        Request httpRequest = new Request.Builder()
                .header("Authorization", "Bearer " + apiKey)
                .url(ValidateUtil.concatUrl(baseUrl, qwenConfig.getDashscopeEmbeddingUrl()))
                .post(RequestBody.create(jsonString, MediaType.parse(Constants.APPLICATION_JSON)))
                .build();

        Response execute = okHttpClient.newCall(httpRequest).execute();
        if (execute.isSuccessful() && execute.body() != null) {
            QwenEmbeddingResponse qwenResponse = mapper.readValue(execute.body().string(), QwenEmbeddingResponse.class);
            return toOpenAiEmbeddingResponse(qwenResponse, request.getModel());
        }
        return null;
    }

    private List<String> resolveInputTexts(EmbeddingRequest request) {
        Object input = request.getInput();
        List<String> texts = new ArrayList<>();
        if (input instanceof String) {
            texts.add((String) input);
            return texts;
        }
        if (input instanceof List) {
            for (Object item : (List<?>) input) {
                texts.add(String.valueOf(item));
            }
            return texts;
        }
        if (input instanceof String[]) {
            for (String item : (String[]) input) {
                texts.add(item);
            }
            return texts;
        }
        throw new IllegalArgumentException("Qwen embedding input must be a string or list of strings.");
    }

    private EmbeddingResponse toOpenAiEmbeddingResponse(QwenEmbeddingResponse qwenResponse, String model) {
        EmbeddingResponse response = new EmbeddingResponse();
        response.setModel(model);

        List<Embedding> data = new ArrayList<>();
        if (qwenResponse != null && qwenResponse.getOutput() != null && qwenResponse.getOutput().getEmbeddings() != null) {
            int index = 0;
            for (QwenEmbeddingResponse.Embedding embedding : qwenResponse.getOutput().getEmbeddings()) {
                Integer resolvedIndex = embedding.getTextIndex() != null ? embedding.getTextIndex() : index;
                data.add(Embedding.builder()
                        .index(resolvedIndex)
                        .embedding(embedding.getEmbedding())
                        .object("embedding")
                        .build());
                index++;
            }
        }
        response.setData(data);
        response.setUsage(toOpenAiUsage(qwenResponse != null ? qwenResponse.getUsage() : null));
        return response;
    }

    private Usage toOpenAiUsage(QwenEmbeddingResponse.Usage usage) {
        if (usage == null) {
            return null;
        }
        Usage mapped = new Usage();
        mapped.setPromptTokens(usage.getInputTokens());
        mapped.setCompletionTokens(0);
        if (usage.getTotalTokens() != null) {
            mapped.setTotalTokens(usage.getTotalTokens());
        } else {
            mapped.setTotalTokens(usage.getInputTokens());
        }
        return mapped;
    }
}
