package com.bobo.llm4j.network;

import okhttp3.Dispatcher;

/**
 * @Author bo
 * @Description Dispatcher提供�?
 * @Date 2024/10/16 23:09
 */
public interface DispatcherProvider {
    Dispatcher getDispatcher();
}
