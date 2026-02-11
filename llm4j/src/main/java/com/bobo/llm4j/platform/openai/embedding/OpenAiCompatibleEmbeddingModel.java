package com.bobo.llm4j.platform.openai.embedding;

import com.bobo.llm4j.constant.Constants;
import com.bobo.llm4j.rag.embedding.EmbeddingModel;
import com.bobo.llm4j.utils.ValidateUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI-compatible embedding client used by both OpenAI and Qwen.
 */
public class OpenAiCompatibleEmbeddingModel implements EmbeddingModel {

    private final OkHttpClient okHttpClient;
    private final String apiHost;
    private final String apiKey;
    private final String embeddingUrl;
    private final String model;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenAiCompatibleEmbeddingModel(OkHttpClient okHttpClient, String apiHost, String apiKey,
                                          String embeddingUrl, String model) {
        if (okHttpClient == null) {
            throw new IllegalArgumentException("okHttpClient cannot be null");
        }
        this.okHttpClient = okHttpClient;
        this.apiHost = apiHost;
        this.apiKey = apiKey;
        this.embeddingUrl = embeddingUrl;
        this.model = model;
    }

    @Override
    public List<List<Double>> embedAll(List<String> texts) throws Exception {
        if (texts == null || texts.isEmpty()) {
            return new ArrayList<List<Double>>();
        }
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("model", model);
        payload.put("input", texts);

        String body = objectMapper.writeValueAsString(payload);
        Request request = new Request.Builder()
                .header("Authorization", "Bearer " + apiKey)
                .url(ValidateUtil.concatUrl(apiHost, embeddingUrl))
                .post(RequestBody.create(body, MediaType.parse(Constants.JSON_CONTENT_TYPE)))
                .build();

        Response response = okHttpClient.newCall(request).execute();
        if (!response.isSuccessful() || response.body() == null) {
            throw new RuntimeException("Embedding request failed, status=" + response.code());
        }

        String responseBody = response.body().string();
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode dataNode = root.get("data");
        List<List<Double>> result = new ArrayList<List<Double>>();
        if (dataNode == null || !dataNode.isArray()) {
            return result;
        }
        for (JsonNode item : dataNode) {
            JsonNode embedding = item.get("embedding");
            if (embedding == null || !embedding.isArray()) {
                continue;
            }
            List<Double> vector = new ArrayList<Double>(embedding.size());
            for (JsonNode value : embedding) {
                vector.add(value.asDouble());
            }
            result.add(vector);
        }
        return result;
    }
}

