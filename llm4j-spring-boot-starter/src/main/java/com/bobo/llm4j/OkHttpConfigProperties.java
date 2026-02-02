package com.bobo.llm4j;

import lombok.Data;
import okhttp3.logging.HttpLoggingInterceptor;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.Proxy;
import java.util.concurrent.TimeUnit;

/**
 * @Author cly
 * @Description OkHttp配置文件
 * @Date 2024/8/10 0:49
 */

@Data
@ConfigurationProperties(prefix = "ai.okhttp")
public class OkHttpConfigProperties {

    private Proxy.Type proxyType = Proxy.Type.HTTP;
    private String proxyUrl = "";
    private int proxyPort;

    private HttpLoggingInterceptor.Level log = HttpLoggingInterceptor.Level.BASIC;
    private int connectTimeout = 300;
    private int writeTimeout = 300;
    private int readTimeout = 300;
    private TimeUnit timeUnit = TimeUnit.SECONDS;

    /**
     * 忽略SSL证书，用于请求Moonshot(Kimi)，其它平台可以不用忽�?
     */
    private boolean ignoreSsl = true;
}
