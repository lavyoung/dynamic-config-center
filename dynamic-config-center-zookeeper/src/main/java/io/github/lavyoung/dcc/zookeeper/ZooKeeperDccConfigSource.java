package io.github.lavyoung.dcc.zookeeper;

import io.github.lavyoung.dcc.core.DccConfigSource;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Consumer;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.framework.recipes.cache.CuratorCacheListener;

/**
 * 基于 ZooKeeper CuratorCache 的动态配置数据源。
 *
 * <p>本类是 ZooKeeper 对 {@link DccConfigSource} 的实现，core 模块不依赖本类。
 *
 * @author lavyoung
 * @since 0.1.0
 */
public final class ZooKeeperDccConfigSource implements DccConfigSource {

    private final CuratorFramework client;
    private final String rootPath;
    private final java.util.List<CuratorCache> caches = new java.util.concurrent.CopyOnWriteArrayList<>();

    /**
     * 创建 ZooKeeper 配置数据源。
     *
     * @param client Curator 客户端
     * @param rootPath 配置根路径
     * @throws NullPointerException     参数为空时抛出
     * @throws IllegalArgumentException 根路径不是绝对路径时抛出
     */
    public ZooKeeperDccConfigSource(CuratorFramework client, String rootPath) {
        this.client = Objects.requireNonNull(client, "client must not be null");
        this.rootPath = requireRootPath(rootPath);
    }

    /**
     * 监听指定配置键。
     *
     * @param key 配置键
     * @param valueConsumer 配置值回调
     * @throws Exception 监听器启动失败时抛出
     */
    @Override
    public AutoCloseable watch(String key, Consumer<String> valueConsumer) throws Exception {
        Objects.requireNonNull(valueConsumer, "valueConsumer must not be null");
        String path = rootPath + "/" + requireKey(key);
        CuratorCache cache = CuratorCache.build(client, path);
        CuratorCacheListener listener = CuratorCacheListener.builder()
                .forCreates(data -> notifyIfTarget(data, path, valueConsumer))
                .forChanges((oldData, newData) -> notifyIfTarget(newData, path, valueConsumer))
                .forInitialized(() -> cache.get(path).ifPresent(data -> notifyIfTarget(data, path, valueConsumer)))
                .build();
        cache.listenable().addListener(listener);
        caches.add(cache);
        cache.start();
        return () -> {
            cache.close();
            caches.remove(cache);
        };
    }

    private static void notifyIfTarget(ChildData data, String path, Consumer<String> consumer) {
        if (data != null && path.equals(data.getPath())) {
            consumer.accept(new String(data.getData(), StandardCharsets.UTF_8));
        }
    }

    private static String requireRootPath(String path) {
        Objects.requireNonNull(path, "rootPath must not be null");
        String normalized = path.trim();
        if (normalized.isBlank() || !normalized.startsWith("/")) {
            throw new IllegalArgumentException("rootPath must be an absolute path");
        }
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }

    private static String requireKey(String key) {
        Objects.requireNonNull(key, "key must not be null");
        if (key.isBlank() || key.contains("/")) {
            throw new IllegalArgumentException("key must not be blank or contain '/'");
        }
        return key.trim();
    }

    /**
     * 关闭所有配置节点缓存。
     */
    @Override
    public void close() {
        caches.forEach(CuratorCache::close);
        caches.clear();
    }
}
