package com.bobo.llm4j.interceptor;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @Author cly
 * @Description TODO
 * @Date 2024/9/20 18:56
 */
public class ContentTypeInterceptor implements Interceptor {

    @Override
    public Response intercept(Chain chain) throws IOException {
        // 发起请求并获取响�?
        Response response = chain.proceed(chain.request());

        // 检查Content-Type是否为application/x-ndjson
        if (response.header("Content-Type") != null &&
                response.header("Content-Type").contains("application/x-ndjson")) {

            // 获取原始响应�?
            ResponseBody responseBody = response.body();
            BufferedSource source = responseBody.source();
            source.request(Long.MAX_VALUE); // 缓冲整个响应�?
            Buffer buffer = source.getBuffer();

            // 读取响应体并将其按换行符分割，模拟处�?application/x-ndjson -> text/event-stream
            String bodyString = buffer.clone().readString(StandardCharsets.UTF_8);
            String[] ndjsonLines = bodyString.split("\n");

            StringBuilder sseBody = new StringBuilder();
            for (String jsonLine : ndjsonLines) {
                if (!jsonLine.trim().isEmpty()) {
                    // 这里简单处理，将ndjson的每一行当作SSE事件的data部分
                    sseBody.append("data: ").append(jsonLine).append("\n\n");
                }
            }

            // 创建新的响应体，替换掉原有的内容
            ResponseBody modifiedBody = ResponseBody.create(
                    MediaType.get("text/event-stream"),
                    sseBody.toString()
            );

            // 返回修改后的响应，更新了Content-Type和响应体
            return response.newBuilder()
                    .header("Content-Type", "text/event-stream")
                    .body(modifiedBody)
                    .build();
        }

        return response;
    }

}
