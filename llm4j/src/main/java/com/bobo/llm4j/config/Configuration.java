package com.bobo.llm4j.config;

import lombok.Builder;
import lombok.Data;
import okhttp3.OkHttpClient;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSources;

import java.util.concurrent.TimeUnit;


/**
 * @Author bo
 * @Description 统一的配置管理
 */

@Data
@Builder
public class Configuration {

    @Builder.Default
    private OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    public EventSource.Factory createRequestFactory() {
        return EventSources.createFactory(okHttpClient);
    }

    private OpenAiConfig openAiConfig;
    private QwenConfig qwenConfig;
}
