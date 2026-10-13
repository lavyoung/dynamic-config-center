package io.github.lavyoung.dcc.zookeeper;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.test.TestingServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ZooKeeper 配置数据源集成测试。
 *
 * <p>使用内嵌 ZooKeeper 验证节点初始化和动态修改事件，避免依赖外部环境。
 *
 * @author lavyoung
 * @since 0.1.0
 */
class ZooKeeperDccConfigSourceTest {

    private static TestingServer server;
    private static CuratorFramework client;

    /**
     * 启动内嵌 ZooKeeper 和 Curator 客户端。
     *
     * @throws Exception 启动失败时抛出
     */
    @BeforeAll
    static void setUp() throws Exception {
        server = new TestingServer(true);
        client = CuratorFrameworkFactory.builder()
                .connectString(server.getConnectString())
                .retryPolicy(new ExponentialBackoffRetry(100, 3))
                .build();
        client.start();
        client.blockUntilConnected(10, TimeUnit.SECONDS);
    }

    /**
     * 关闭测试资源。
     *
     * @throws Exception 关闭失败时抛出
     */
    @AfterAll
    static void tearDown() throws Exception {
        client.close();
        server.close();
    }

    /**
     * 验证节点创建和修改会推送 UTF-8 配置值。
     *
     * @throws Exception Curator 操作失败时抛出
     */
    @Test
    void shouldWatchCreatedAndChangedValues() throws Exception {
        String path = "/dcc-test/feature.enabled";
        CountDownLatch latch = new CountDownLatch(2);
        AtomicReference<String> lastValue = new AtomicReference<>();
        ZooKeeperDccConfigSource source = new ZooKeeperDccConfigSource(
                client,
                "/dcc-test"
        );

        try {
            source.watch("feature.enabled", value -> {
                lastValue.set(value);
                latch.countDown();
            });

            client.create().creatingParentsIfNeeded().forPath(
                    path,
                    "开启".getBytes(StandardCharsets.UTF_8)
            );
            client.setData().forPath(
                    path,
                    "关闭".getBytes(StandardCharsets.UTF_8)
            );

            assertTrue(latch.await(10, TimeUnit.SECONDS));
            assertEquals("关闭", lastValue.get());
        } finally {
            source.close();
        }
    }
}
