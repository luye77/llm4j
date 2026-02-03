package com.bobo.llm4j.network.impl;

import com.bobo.llm4j.network.DispatcherProvider;
import okhttp3.Dispatcher;

/**
 * @Author bo
 * @Description Dispatcher默认实现
 * @Date 2024/10/16 23:11
 */
public class DefaultDispatcherProvider implements DispatcherProvider {
    @Override
    public Dispatcher getDispatcher() {
        return new Dispatcher();
    }
}
