package com.bobo.llm4j.network.impl;

import com.bobo.llm4j.network.ConnectionPoolProvider;
import okhttp3.ConnectionPool;

/**
 * @Author bo
 * @Description ConnectionPool默认实现
 * @Date 2024/10/16 23:11
 */
public class DefaultConnectionPoolProvider implements ConnectionPoolProvider {
    @Override
    public ConnectionPool getConnectionPool() {
        return new ConnectionPool();
    }
}
