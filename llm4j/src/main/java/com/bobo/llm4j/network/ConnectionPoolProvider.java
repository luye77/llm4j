package com.bobo.llm4j.network;

import okhttp3.ConnectionPool;

/**
 * @Author cly
 * @Description ConnectionPool提供�?
 * @Date 2024/10/16 23:10
 */
public interface ConnectionPoolProvider {
    ConnectionPool getConnectionPool();
}
