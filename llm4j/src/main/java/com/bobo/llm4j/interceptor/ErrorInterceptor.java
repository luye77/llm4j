package com.bobo.llm4j.interceptor;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.bobo.llm4j.exception.CommonException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @Author bo
 * @Description 错误处理
 * @Date 2024/8/29 14:55
 */
@Slf4j
public class ErrorInterceptor implements Interceptor {
    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {
        Request original = chain.request();
        Response response = chain.proceed(original);

        // 判断是否为流式响应，流式响应Content-Type为text/event-stream
        if (isStreamingResponse(response)) {
            return response; // 直接返回，不处理流式响应
        }

        if (!response.isSuccessful() && (response.code() != 100 && response.code() != 101)) {
            ResponseBody responseBody = response.body();
            String errorMsg = getResponseBodyContent(responseBody);

            try {
                JSONObject object = JSON.parseObject(errorMsg);
                if (object == null) {
                    errorMsg = response.code() + " " + response.message();
                    throw new CommonException(errorMsg);
                }
            } catch (Exception e) {
                throw new CommonException(errorMsg);
            }

            throw new CommonException("AI服务请求异常：" + errorMsg);
        } else {

            // 处理腾讯混元部分
            ResponseBody responseBody = response.body();
            byte[] contentBytes = getResponseBodyBytes(responseBody);
            String content = new String(contentBytes, StandardCharsets.UTF_8);
            if (content.contains("Response") && content.contains("Error")) {
                JSONObject errorObject = JSON.parseObject(content);
                throw new CommonException("AI服务请求异常：" +
                        (errorObject != null ? errorObject.toJSONString() : content));
            }
            // 重新构建响应体，确保内容可用
            assert responseBody != null;
            ResponseBody newBody = ResponseBody.create(responseBody.contentType(), contentBytes);
            return response.newBuilder().body(newBody).build();
        }

    }

    private boolean isStreamingResponse(Response response) {
        assert response.body() != null;
        MediaType contentType = response.body().contentType();
        return contentType != null && ( contentType.toString().contains("text/event-stream") || contentType.toString().contains("application/x-ndjson") );
    }

    private String getResponseBodyContent(ResponseBody responseBody) throws IOException {
        if (responseBody == null) return "";
        byte[] contentBytes = responseBody.bytes();
        return new String(contentBytes, StandardCharsets.UTF_8);
    }

    private byte[] getResponseBodyBytes(ResponseBody responseBody) throws IOException {
        if (responseBody == null) return new byte[0];
        return responseBody.bytes();
    }
}