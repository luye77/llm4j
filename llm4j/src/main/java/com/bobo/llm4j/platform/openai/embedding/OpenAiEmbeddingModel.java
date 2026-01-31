package com.bobo.llm4j.platform.openai.embedding;

import com.alibaba.fastjson2.JSON;
import com.bobo.llm4j.config.OpenAiConfig;
import com.bobo.llm4j.constant.Constants;
import com.bobo.llm4j.platform.openai.embedding.entity.EmbeddingRequest;
import com.bobo.llm4j.platform.openai.embedding.entity.EmbeddingResponse;
import com.bobo.llm4j.service.Configuration;
import com.bobo.llm4j.service.EmbeddingModel;
import com.bobo.llm4j.utils.ValidateUtil;
import okhttp3.*;

/**
 * OpenAiEmbeddingModel - OpenAI Embedding模型实现 (对应Spring AI的OpenAiEmbeddingModel)
 */
public class OpenAiEmbeddingModel implements EmbeddingModel {

    private final OpenAiConfig openAiConfig;
    private final OkHttpClient okHttpClient;

    public OpenAiEmbeddingModel(Configuration configuration) {
        this.openAiConfig = configuration.getOpenAiConfig();
        this.okHttpClient = configuration.getOkHttpClient();
    }

    public OpenAiEmbeddingModel(Configuration configuration, OpenAiConfig openAiConfig) {
        this.openAiConfig = openAiConfig;
        this.okHttpClient = configuration.getOkHttpClient();
    }

    @Override
    public EmbeddingResponse call(String baseUrl, String apiKey, EmbeddingRequest request) throws Exception {
        if(baseUrl == null || "".equals(baseUrl)) baseUrl = openAiConfig.getApiHost();
        if(apiKey == null || "".equals(apiKey)) apiKey = openAiConfig.getApiKey();
        String jsonString = JSON.toJSONString(request);

        Request httpRequest = new Request.Builder()
                .header("Authorization", "Bearer " + apiKey)
                .url(ValidateUtil.concatUrl(baseUrl, openAiConfig.getEmbeddingUrl()))
                .post(RequestBody.create(MediaType.parse(Constants.APPLICATION_JSON), jsonString))
                .build();
        Response execute = okHttpClient.newCall(httpRequest).execute();
        if (execute.isSuccessful() && execute.body() != null) {
            return JSON.parseObject(execute.body().string(), EmbeddingResponse.class);
        }
        return null;
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) throws Exception {
        return call(null, null, request);
    }
}
