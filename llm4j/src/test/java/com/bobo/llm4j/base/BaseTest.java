package com.bobo.llm4j.base;

import com.bobo.llm4j.config.OpenAiConfig;
import com.bobo.llm4j.interceptor.ContentTypeInterceptor;
import com.bobo.llm4j.network.ConnectionPoolProvider;
import com.bobo.llm4j.network.DispatcherProvider;
import com.bobo.llm4j.service.ChatModel;
import com.bobo.llm4j.service.Configuration;
import com.bobo.llm4j.service.EmbeddingModel;
import com.bobo.llm4j.service.factor.AiService;
import com.bobo.llm4j.utils.OkHttpUtil;
import com.bobo.llm4j.utils.ServiceLoaderUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.junit.Before;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

/**
 * 测试基类
 * <p>
 * 提供所有测试类的通用初始化逻辑，包括：
 * <ul>
 *     <li>OpenAI配置初始化</li>
 *     <li>OkHttp客户端配置</li>
 *     <li>AI服务工厂创建</li>
 * </ul>
 * </p>
 *
 * @author bobo
 * @since 1.4.3
 */
@Slf4j
public abstract class BaseTest {

    /**
     * API服务地址
     */
    protected static final String API_HOST = "https://api.openai.com/";

    /**
     * API密钥
     */
    protected static final String API_KEY = "sk-your-api-key";

    /**
     * 默认聊天模型
     */
    protected static final String CHAT_MODEL = "gpt-4o-mini";

    /**
     * 默认嵌入模型
     */
    protected static final String EMBEDDING_MODEL = "text-embedding-ada-002";

    /**
     * 是否启用代理
     */
    protected static final boolean USE_PROXY = false;

    /**
     * 代理主机地址
     */
    protected static final String PROXY_HOST = "127.0.0.1";

    /**
     * 代理端口
     */
    protected static final int PROXY_PORT = 10809;

    /**
     * 连接超时时间（秒）
     */
    protected static final int CONNECT_TIMEOUT = 300;

    /**
     * 读取超时时间（秒）
     */
    protected static final int READ_TIMEOUT = 300;

    /**
     * 写入超时时间（秒）
     */
    protected static final int WRITE_TIMEOUT = 300;

    /**
     * AI服务工厂
     */
    protected AiService aiService;

    /**
     * 嵌入模型
     */
    protected EmbeddingModel embeddingModel;

    /**
     * 聊天模型
     */
    protected ChatModel chatModel;

    /**
     * 统一配置
     */
    protected Configuration configuration;

    /**
     * 测试前初始化
     * <p>
     * 初始化AI服务所需的所有组件
     * </p>
     *
     * @throws NoSuchAlgorithmException 算法不存在异常
     * @throws KeyManagementException   密钥管理异常
     */
    @Before
    public void setUp() throws NoSuchAlgorithmException, KeyManagementException {
        initConfiguration();
        initOkHttpClient();
        initAiService();
        log.info("测试环境初始化完成");
    }

    /**
     * 初始化OpenAI配置
     */
    private void initConfiguration() {
        OpenAiConfig openAiConfig = new OpenAiConfig();
        openAiConfig.setApiHost(API_HOST);
        openAiConfig.setApiKey(API_KEY);

        configuration = new Configuration();
        configuration.setOpenAiConfig(openAiConfig);
    }

    /**
     * 初始化OkHttp客户端
     *
     * @throws NoSuchAlgorithmException 算法不存在异常
     * @throws KeyManagementException   密钥管理异常
     */
    private void initOkHttpClient() throws NoSuchAlgorithmException, KeyManagementException {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);

        DispatcherProvider dispatcherProvider = ServiceLoaderUtil.load(DispatcherProvider.class);
        ConnectionPoolProvider poolProvider = ServiceLoaderUtil.load(ConnectionPoolProvider.class);

        Dispatcher dispatcher = dispatcherProvider.getDispatcher();
        ConnectionPool connectionPool = poolProvider.getConnectionPool();

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .addInterceptor(new ContentTypeInterceptor())
                .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                .dispatcher(dispatcher)
                .connectionPool(connectionPool)
                .sslSocketFactory(
                        OkHttpUtil.getIgnoreInitedSslContext().getSocketFactory(),
                        OkHttpUtil.IGNORE_SSL_TRUST_MANAGER_X509
                )
                .hostnameVerifier(OkHttpUtil.getIgnoreSslHostnameVerifier());

        if (USE_PROXY) {
            builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(PROXY_HOST, PROXY_PORT)));
        }

        configuration.setOkHttpClient(builder.build());
    }

    /**
     * 初始化AI服务
     */
    private void initAiService() {
        aiService = new AiService(configuration);
        embeddingModel = aiService.getEmbeddingModel();
        chatModel = aiService.getChatModel();
    }
}
