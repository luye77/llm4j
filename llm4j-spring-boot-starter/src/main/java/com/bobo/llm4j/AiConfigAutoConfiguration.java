package com.bobo.llm4j;

import com.bobo.llm4j.config.OpenAiConfig;
import com.bobo.llm4j.config.QwenConfig;
import com.bobo.llm4j.interceptor.ContentTypeInterceptor;
import com.bobo.llm4j.interceptor.ErrorInterceptor;
import com.bobo.llm4j.network.ConnectionPoolProvider;
import com.bobo.llm4j.network.DispatcherProvider;
import com.bobo.llm4j.utils.OkHttpUtil;
import com.bobo.llm4j.utils.ServiceLoaderUtil;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

/**
 * @Author bo
 * @Description AI Configuration Auto Configuration
 * @Date 2024/8/9 23:22
 */
@Configuration
@EnableConfigurationProperties({
        OpenAiConfigProperties.class,
        QwenConfigProperties.class,
        OkHttpConfigProperties.class,
})
public class AiConfigAutoConfiguration {

    private final OkHttpConfigProperties okHttpConfigProperties;
    private final OpenAiConfigProperties openAiConfigProperties;
    private final QwenConfigProperties qwenConfigProperties;

    private final com.bobo.llm4j.config.Configuration configuration = new com.bobo.llm4j.config.Configuration();

    public AiConfigAutoConfiguration(OkHttpConfigProperties okHttpConfigProperties,
                                     OpenAiConfigProperties openAiConfigProperties,
                                     QwenConfigProperties qwenConfigProperties) {
        this.okHttpConfigProperties = okHttpConfigProperties;
        this.openAiConfigProperties = openAiConfigProperties;
        this.qwenConfigProperties = qwenConfigProperties;
    }

    @Bean
    public AiService aiService() {
        return new AiService(configuration);
    }

    @PostConstruct
    private void init() {
        initOkHttp();
        initOpenAiConfig();
        initQwenConfig();
    }

    private void initOkHttp() {
        // 日志配置
        HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();
        httpLoggingInterceptor.setLevel(okHttpConfigProperties.getLog());

        // SPI加载dispatcher和connectionPool
        DispatcherProvider dispatcherProvider = ServiceLoaderUtil.load(DispatcherProvider.class);
        ConnectionPoolProvider connectionPoolProvider = ServiceLoaderUtil.load(ConnectionPoolProvider.class);

        // 创建OkHttpClient
        OkHttpClient.Builder okHttpBuilder = new OkHttpClient
                .Builder()
                .addInterceptor(httpLoggingInterceptor)
                .addInterceptor(new ErrorInterceptor())
                .addInterceptor(new ContentTypeInterceptor())
                .connectTimeout(okHttpConfigProperties.getConnectTimeout(), okHttpConfigProperties.getTimeUnit())
                .writeTimeout(okHttpConfigProperties.getWriteTimeout(), okHttpConfigProperties.getTimeUnit())
                .readTimeout(okHttpConfigProperties.getReadTimeout(), okHttpConfigProperties.getTimeUnit())
                .dispatcher(dispatcherProvider.getDispatcher())
                .connectionPool(connectionPoolProvider.getConnectionPool());

        // 是否开启Proxy代理
        if(StringUtils.isNotBlank(okHttpConfigProperties.getProxyUrl())){
            Proxy proxy = new Proxy(okHttpConfigProperties.getProxyType(), new InetSocketAddress(okHttpConfigProperties.getProxyUrl(), okHttpConfigProperties.getProxyPort()));
            okHttpBuilder.proxy(proxy);
        }

        // 忽略SSL证书验证
        if(okHttpConfigProperties.isIgnoreSsl()){
            try {
                okHttpBuilder
                        .sslSocketFactory(OkHttpUtil.getIgnoreInitedSslContext().getSocketFactory(), OkHttpUtil.IGNORE_SSL_TRUST_MANAGER_X509)
                        .hostnameVerifier(OkHttpUtil.getIgnoreSslHostnameVerifier());
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                throw new RuntimeException(e);
            }
        }

        OkHttpClient okHttpClient = okHttpBuilder.build();
        configuration.setOkHttpClient(okHttpClient);
    }

    /**
     * 初始化OpenAI配置信息
     */
    private void initOpenAiConfig() {
        OpenAiConfig openAiConfig = new OpenAiConfig();
        openAiConfig.setApiHost(openAiConfigProperties.getApiHost());
        openAiConfig.setApiKey(openAiConfigProperties.getApiKey());
        openAiConfig.setChatCompletionUrl(openAiConfigProperties.getChatCompletionUrl());
        openAiConfig.setEmbeddingUrl(openAiConfigProperties.getEmbeddingUrl());

        configuration.setOpenAiConfig(openAiConfig);
    }

    /**
     * 初始化通义千问配置信息
     */
    private void initQwenConfig() {
        QwenConfig qwenConfig = new QwenConfig();
        qwenConfig.setApiHost(qwenConfigProperties.getApiHost());
        qwenConfig.setApiKey(qwenConfigProperties.getApiKey());
        qwenConfig.setChatCompletionUrl(qwenConfigProperties.getChatCompletionUrl());
        qwenConfig.setEmbeddingUrl(qwenConfigProperties.getEmbeddingUrl());
        qwenConfig.setCompatibleMode(qwenConfigProperties.isCompatibleMode());
        qwenConfig.setDashscopeChatCompletionUrl(qwenConfigProperties.getDashscopeChatCompletionUrl());
        qwenConfig.setDashscopeMultiModalCompletionUrl(qwenConfigProperties.getDashscopeMultiModalCompletionUrl());
        qwenConfig.setDashscopeEmbeddingUrl(qwenConfigProperties.getDashscopeEmbeddingUrl());

        configuration.setQwenConfig(qwenConfig);
    }
}
