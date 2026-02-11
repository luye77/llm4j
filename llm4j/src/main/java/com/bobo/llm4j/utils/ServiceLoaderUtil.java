package com.bobo.llm4j.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.ServiceLoader;

/**
 * @Author bo
 * @Description SPI服务加载
 */
@Slf4j
public class ServiceLoaderUtil {
    public static <T> T load(Class<T> service) {
        ServiceLoader<T> loader = ServiceLoader.load(service);
        for (T impl : loader) {
            log.info("Loaded SPI implementation: {}", impl.getClass().getSimpleName());
            return impl;
        }
        throw new IllegalStateException("No implementation found for " + service.getName());
    }
}
