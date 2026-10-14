package io.github.lavyoung.dcc.ratelimiter.local;

/**
 * 本地限流存储配置。
 *
 * <p>该 record 用于控制 {@link LocalBucket4jRateLimiterStore} 的本地状态容量。
 * 本地限流实现会在 JVM 内保存限流桶、拒绝计数和临时黑名单，因此必须限制
 * key 数量，避免高基数字段导致内存无限增长。</p>
 *
 * <p>后续 Spring Boot starter 可以将配置文件中的属性绑定为该对象，
 * 从而让业务项目按自身流量规模调整本地限流容量。</p>
 *
 * @param maxBucketKeys                最多保存的限流桶 key 数量
 * @param maxRejectCounterKeys         最多保存的拒绝计数 key 数量
 * @param maxBlacklistKeys             最多保存的临时黑名单 key 数量
 * @param failOpenWhenCapacityExceeded 容量达到上限且遇到新 key 时是否放行
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 0.2.0
 * @date 2026/10/14
 */
public record LocalRateLimiterOptions(
        int maxBucketKeys,
        int maxRejectCounterKeys,
        int maxBlacklistKeys,
        boolean failOpenWhenCapacityExceeded
) {
    /**
     * 创建默认本地限流配置。
     *
     * <p>默认容量面向普通业务服务的本地保护场景。高流量服务后续应通过
     * starter 配置显式调整容量，或切换为 Redis 分布式限流实现。</p>
     *
     * @return 默认本地限流配置
     */
    public static LocalRateLimiterOptions defaults() {
        return new LocalRateLimiterOptions(
                100_000,
                100_000,
                100_000,
                true
        );
    }

    /**
     * 校验本地限流配置。
     *
     * @throws IllegalArgumentException 当任意容量配置小于 1 时抛出
     */
    public LocalRateLimiterOptions {
        if (maxBucketKeys < 1) {
            throw new IllegalArgumentException("maxBucketKeys must be positive");
        }
        if (maxRejectCounterKeys < 1) {
            throw new IllegalArgumentException("maxRejectCounterKeys must be positive");
        }
        if (maxBlacklistKeys < 1) {
            throw new IllegalArgumentException("maxBlacklistKeys must be positive");
        }
    }
}
