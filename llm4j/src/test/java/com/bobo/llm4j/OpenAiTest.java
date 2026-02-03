package com.bobo.llm4j;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.bobo.llm4j.annotation.Tool;
import com.bobo.llm4j.annotation.ToolParam;
import com.bobo.llm4j.annotation.ToolRequest;
import com.bobo.llm4j.platform.openai.tool.ToolDefinition;
import com.bobo.llm4j.utils.ToolUtil;
import com.bobo.llm4j.config.OpenAiConfig;
import com.bobo.llm4j.interceptor.ContentTypeInterceptor;
import com.bobo.llm4j.listener.StreamingResponseHandler;
import com.bobo.llm4j.network.ConnectionPoolProvider;
import com.bobo.llm4j.network.DispatcherProvider;
import com.bobo.llm4j.platform.openai.chat.entity.*;
import com.bobo.llm4j.platform.openai.embedding.entity.EmbeddingRequest;
import com.bobo.llm4j.platform.openai.embedding.entity.EmbeddingResponse;
import com.bobo.llm4j.service.*;
import com.bobo.llm4j.service.factor.AiService;
import com.bobo.llm4j.utils.OkHttpUtil;
import com.bobo.llm4j.utils.ServiceLoaderUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.logging.HttpLoggingInterceptor;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * OpenAi测试类
 */
@Slf4j
public class OpenAiTest {

    private EmbeddingModel embeddingModel;
    private ChatModel chatModel;

    @Before
    public void test_init() throws NoSuchAlgorithmException, KeyManagementException {
        OpenAiConfig openAiConfig = new OpenAiConfig();
        openAiConfig.setApiHost("************");
        openAiConfig.setApiKey("*************");

        Configuration configuration = new Configuration();
        configuration.setOpenAiConfig(openAiConfig);

        HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();
        httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);
        DispatcherProvider dispatcherProvider = ServiceLoaderUtil.load(DispatcherProvider.class);
        ConnectionPoolProvider connectionPoolProvider = ServiceLoaderUtil.load(ConnectionPoolProvider.class);
        Dispatcher dispatcher = dispatcherProvider.getDispatcher();
        ConnectionPool connectionPool = connectionPoolProvider.getConnectionPool();

        OkHttpClient okHttpClient = new OkHttpClient
                .Builder()
                .addInterceptor(httpLoggingInterceptor)
                .addInterceptor(new ContentTypeInterceptor())
                .connectTimeout(300, TimeUnit.SECONDS)
                .writeTimeout(300, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)
                .dispatcher(dispatcher)
                .connectionPool(connectionPool)
                .sslSocketFactory(OkHttpUtil.getIgnoreInitedSslContext().getSocketFactory(), OkHttpUtil.IGNORE_SSL_TRUST_MANAGER_X509)
                .hostnameVerifier(OkHttpUtil.getIgnoreSslHostnameVerifier())
                .proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 10809)))
                .build();
        configuration.setOkHttpClient(okHttpClient);

        AiService aiService = new AiService(configuration);

        embeddingModel = aiService.getEmbeddingModel();
        chatModel = aiService.getChatModel();
    }


    @Test
    public void test_embed() throws Exception {
        EmbeddingRequest request = EmbeddingRequest.builder()
                .input("The food was delicious and the waiter...")
                .model("text-embedding-ada-002")
                .build();
        System.out.println(request);

        EmbeddingResponse response = embeddingModel.call(null, null, request);

        System.out.println(response);
    }


    @Test
    public void test_chat_common() throws Exception {
        Prompt prompt = Prompt.builder()
                .model("gpt-4o-mini")
                .message(Message.withUser("鲁迅为什么打周树人"))
                .build();

        System.out.println("请求参数");
        System.out.println(prompt);

        ChatResponse chatResponse = chatModel.call(prompt);

        System.out.println("请求成功");
        System.out.println(chatResponse);
    }

    @Test
    public void test_chat_history() throws Exception {
        List<Message> history = new ArrayList<>();

        Prompt prompt = Prompt.builder()
                .model("gpt-4o-mini")
                .message(Message.withUser("鲁迅为什么打周树人"))
                .build();

        System.out.println("请求参数");
        System.out.println(prompt);

        history.add(prompt.getMessages().get(prompt.getMessages().size()-1));

        ChatResponse chatResponse = chatModel.call(prompt);

        System.out.println("请求成功");
        System.out.println(chatResponse.getGenerations().get(0).getMessage());
        history.add(chatResponse.getGenerations().get(0).getMessage());

        history.add(Message.withUser("我刚刚问了什么问题"));
        Prompt promptWithHistory = Prompt.builder()
                .model("gpt-4o-mini")
                .messages(history)
                .build();
        ChatResponse responseWithHistory = chatModel.call(promptWithHistory);

        System.out.println("请求成功");
        System.out.println(responseWithHistory);
    }

    @Test
    public void test_chat_multimodal() throws Exception {
        Prompt prompt = Prompt.builder()
                .model("gpt-4o-mini")
                .message(Message.withUser("这几张图片，分别有什么动物 并且是什么品种",
                        "https://tse2-mm.cn.bing.net/th/id/OIP-C.SVxZtXIcz3LbcE4ZeS6jEgHaE7?w=231&h=180&c=7&r=0&o=5&dpr=1.3&pid=1.7",
                        "https://ts3.cn.mm.bing.net/th?id=OIP-C.BYyILFgs3ATnTEQ-B5ApFQHaFj&w=288&h=216&c=8&rs=1&qlt=90&o=6&dpr=1.3&pid=3.1&rm=2"))
                .build();

        System.out.println("请求参数");
        System.out.println(prompt);
        System.out.println(new ObjectMapper().writeValueAsString(prompt));

        ChatResponse chatResponse = chatModel.call(prompt);

        System.out.println("请求成功");
        System.out.println(chatResponse);
    }

    @Test
    public void test_chat_stream() throws Exception {
        Prompt prompt = Prompt.builder()
                .model("gpt-4o-mini")
                .message(Message.withUser("请思考，先有鸡还是先有蛋"))
                .build();

        System.out.println("请求参数");
        System.out.println(prompt);

        StreamingResponseHandler handler = new StreamingResponseHandler() {
            @Override
            protected void send() {
                log.info(this.getCurrStr());
            }
        };

        long currentTimeMillis = System.currentTimeMillis();
        log.info("开始请求");
        chatModel.stream(prompt, handler);

        log.info("请求结束");
        long endTime = System.currentTimeMillis();
        System.out.println(endTime - currentTimeMillis);

        System.out.println(handler.getOutput());
        System.out.println(handler.getUsage());
    }

    @Test
    public void test_chat_function() throws Exception {
        Prompt prompt = Prompt.builder()
                .model("gpt-4o-mini")
                .message(Message.withUser("查询洛阳明天的天气"))
                .functions("queryWeather")
                .build();

        System.out.println("请求参数");
        ObjectMapper objectMapper = new ObjectMapper();
        System.out.println(objectMapper.writeValueAsString(prompt));

        ChatResponse chatResponse = chatModel.call(prompt);

        System.out.println("请求成功");
        System.out.println(chatResponse);
    }

    @Test
    public void test_chat_stream_function() throws Exception {
        Prompt prompt = Prompt.builder()
                .model("gpt-4o-mini")
                .message(Message.withUser("查询洛阳明天的天气"))
                .functions("queryWeather")
                .build();

        StreamingResponseHandler handler = new StreamingResponseHandler() {
            @Override
            protected void send() {
                System.out.println(this.getCurrStr());
            }
        };
        handler.setShowToolArgs(true);

        chatModel.stream(prompt, handler);
        System.out.println("完整内容：");
        System.out.println(handler.getOutput());
        System.out.println("Token使用：");
        System.out.println(handler.getUsage());
    }

    @Tool(name = "queryWeather", description = "查询天气信息")
    public static class QueryWeatherFunction implements java.util.function.Function<QueryWeatherFunction.Request, String> {

        @lombok.Data
        @ToolRequest
        public static class Request {
            @ToolParam(description = "城市名称")
            private String city;
            
            @ToolParam(description = "日期")
            private String date;
        }

        @Override
        public String apply(Request request) {
            return "城市: " + request.city + ", 日期: " + request.date + ", 天气: 晴天, 温度: 25°C";
        }
    }

    @Test
    public void testToolSchemaGeneration() {
        System.out.println("=== 测试Tool Schema生成 ===");

        try {
            ToolDefinition.Function function = ToolUtil.getFunctionDefinition("queryWeather");
            if (function != null) {
                System.out.println("工具生成成功:");
                System.out.println("名称: " + function.getName());
                System.out.println("描述: " + function.getDescription());
                System.out.println("参数: " + function.getParameters());
            } else {
                System.out.println("工具生成失败");
            }

            System.out.println("=== 测试完成 ===");

        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
        }
    }
}
