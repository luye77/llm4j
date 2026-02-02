package com.bobo.llm4j.spi;

import com.bobo.llm4j.network.ConnectionPoolProvider;
import com.bobo.llm4j.network.DispatcherProvider;
import com.bobo.llm4j.utils.ServiceLoaderUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import org.junit.Assert;
import org.junit.Test;

/**
 * ServiceLoader SPI机制测试类
 * <p>
 * 测试框架的SPI扩展机制，包括：
 * <ul>
 *     <li>DispatcherProvider加载</li>
 *     <li>ConnectionPoolProvider加载</li>
 *     <li>自定义SPI实现</li>
 * </ul>
 * </p>
 *
 * @author bobo
 * @since 1.4.3
 */
@Slf4j
public class ServiceLoaderTest {

    /**
     * 测试DispatcherProvider加载
     */
    @Test
    public void testDispatcherProviderLoading() {
        log.info("=== 测试DispatcherProvider加载 ===");

        DispatcherProvider provider = ServiceLoaderUtil.load(DispatcherProvider.class);

        Assert.assertNotNull("DispatcherProvider不应为空", provider);

        Dispatcher dispatcher = provider.getDispatcher();
        Assert.assertNotNull("Dispatcher不应为空", dispatcher);

        log.info("Dispatcher加载成功");
        log.info("最大请求数: {}", dispatcher.getMaxRequests());
        log.info("每主机最大请求数: {}", dispatcher.getMaxRequestsPerHost());
    }

    /**
     * 测试ConnectionPoolProvider加载
     */
    @Test
    public void testConnectionPoolProviderLoading() {
        log.info("=== 测试ConnectionPoolProvider加载 ===");

        ConnectionPoolProvider provider = ServiceLoaderUtil.load(ConnectionPoolProvider.class);

        Assert.assertNotNull("ConnectionPoolProvider不应为空", provider);

        ConnectionPool pool = provider.getConnectionPool();
        Assert.assertNotNull("ConnectionPool不应为空", pool);

        log.info("ConnectionPool加载成功");
        log.info("空闲连接数: {}", pool.idleConnectionCount());
        log.info("总连接数: {}", pool.connectionCount());
    }

    /**
     * 测试重复加载返回相同实例
     */
    @Test
    public void testServiceLoaderCaching() {
        log.info("=== 测试ServiceLoader缓存 ===");

        DispatcherProvider provider1 = ServiceLoaderUtil.load(DispatcherProvider.class);
        DispatcherProvider provider2 = ServiceLoaderUtil.load(DispatcherProvider.class);

        Assert.assertSame("应返回相同实例", provider1, provider2);

        log.info("ServiceLoader缓存验证通过");
    }

    /**
     * 测试Dispatcher配置
     */
    @Test
    public void testDispatcherConfiguration() {
        log.info("=== 测试Dispatcher配置 ===");

        DispatcherProvider provider = ServiceLoaderUtil.load(DispatcherProvider.class);
        Dispatcher dispatcher = provider.getDispatcher();

        // 验证默认配置
        Assert.assertTrue("最大请求数应大于0", dispatcher.getMaxRequests() > 0);
        Assert.assertTrue("每主机最大请求数应大于0", dispatcher.getMaxRequestsPerHost() > 0);

        log.info("最大请求数: {}", dispatcher.getMaxRequests());
        log.info("每主机最大请求数: {}", dispatcher.getMaxRequestsPerHost());
    }

    /**
     * 测试ConnectionPool配置
     */
    @Test
    public void testConnectionPoolConfiguration() {
        log.info("=== 测试ConnectionPool配置 ===");

        ConnectionPoolProvider provider = ServiceLoaderUtil.load(ConnectionPoolProvider.class);
        ConnectionPool pool = provider.getConnectionPool();

        // 验证连接池状态
        Assert.assertTrue("空闲连接数应大于等于0", pool.idleConnectionCount() >= 0);
        Assert.assertTrue("总连接数应大于等于0", pool.connectionCount() >= 0);

        log.info("空闲连接数: {}", pool.idleConnectionCount());
        log.info("总连接数: {}", pool.connectionCount());
    }
}
