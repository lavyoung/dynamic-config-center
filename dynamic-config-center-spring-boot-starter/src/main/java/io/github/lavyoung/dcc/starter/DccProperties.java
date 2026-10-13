package io.github.lavyoung.dcc.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * DCC Starter 配置属性。
 *
 * @param enabled 是否启用动态配置
 * @param connectString ZooKeeper 连接地址
 * @param rootPath ZooKeeper 动态配置根路径
 * @param sessionTimeoutMs 会话超时时间
 * @param connectionTimeoutMs 连接超时时间
 * @author lavyoung
 * @since 0.1.0
 */
@ConfigurationProperties(prefix = "dcc")
public record DccProperties(
        boolean enabled,
        String connectString,
        String rootPath,
        int sessionTimeoutMs,
        int connectionTimeoutMs
) {
}
