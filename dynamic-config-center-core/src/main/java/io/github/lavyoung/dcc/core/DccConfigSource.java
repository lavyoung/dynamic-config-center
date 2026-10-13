package io.github.lavyoung.dcc.core;

import java.util.function.Consumer;

/**
 * 动态配置数据源端口。
 *
 * <p>Core 只依赖该接口，不依赖 ZooKeeper、Redis 或其他具体配置中心。
 *
 * @author lavyoung
 * @since 0.1.0
 */
public interface DccConfigSource extends AutoCloseable {

    /**
     * 监听配置键并在首次加载及后续变化时回调。
     *
     * @param key 配置键
     * @param valueConsumer 配置值回调
     * @throws Exception 监听启动失败时抛出
     */
    AutoCloseable watch(String key, Consumer<String> valueConsumer) throws Exception;

    /**
     * 关闭配置数据源。
     */
    @Override
    void close();
}
