package io.github.lavyoung.dcc.ratelimiter.starter.autoconfigure;

import io.github.lavyoung.dcc.ratelimiter.local.LocalRateLimiterOptions;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * DCC 限流组件的 Spring Boot 启动配置。
 *
 * <p>这组配置解决的是“应用启动时如何装配限流组件”的问题，
 * 和 DCC 动态开关不是同一层职责。比如 {@link #enabled()} 决定
 * starter 是否创建限流相关 Bean，而 DCC 动态开关决定运行期间
 * 某一次调用是否真正执行限流判断。</p>
 *
 * @param enabled                      是否在应用启动时启用限流 starter
 * @param failOpenWhenCapacityExceeded 本地限流缓存达到容量上限时是否放行请求
 * @param local                        本地限流缓存容量配置
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 0.2.0
 * @date 2026/10/14
 */
@ConfigurationProperties(prefix = "dcc.rate-limiter")
public record DccRateLimiterProperties(
        boolean enabled,
        boolean failOpenWhenCapacityExceeded,
        Local local
) {
    private static final int DEFAULT_MAX_BUCKET_KEYS = 100_000;
    private static final int DEFAULT_MAX_REJECT_COUNTER_KEYS = 100_000;
    private static final int DEFAULT_MAX_BLACKLIST_KEYS = 100_000;

    /**
     * 创建配置对象时补齐默认值。
     *
     * <p>Spring Boot 绑定配置时，如果用户没有配置 {@code local}，
     * 这里会使用一组保守的默认容量，避免组件因为空配置无法启动。</p>
     */
    public DccRateLimiterProperties {
        if (local == null) {
            local = Local.defaults();
        }
    }

    /**
     * 将 Spring Boot 配置转换为限流核心模块使用的本地存储配置。
     *
     * @return 本地限流器容量与降级策略配置
     */
    public LocalRateLimiterOptions toLocalOptions() {
        return new LocalRateLimiterOptions(
                local.maxBucketKeys(),
                local.maxRejectCounterKeys(),
                local.maxBlacklistKeys(),
                failOpenWhenCapacityExceeded
        );
    }


    /**
     * 本地限流器容量配置。
     *
     * @param maxBucketKeys        最多维护多少个令牌桶 key
     * @param maxRejectCounterKeys 最多维护多少个拒绝计数 key
     * @param maxBlacklistKeys     最多维护多少个黑名单 key
     */
    public record Local(
            int maxBucketKeys,
            int maxRejectCounterKeys,
            int maxBlacklistKeys
    ) {
        /**
         * 构造本地容量配置，并对未配置的数值使用默认值。
         */
        public Local {
            maxBucketKeys = normalize(maxBucketKeys, DEFAULT_MAX_BUCKET_KEYS);
            maxRejectCounterKeys = normalize(maxRejectCounterKeys, DEFAULT_MAX_REJECT_COUNTER_KEYS);
            maxBlacklistKeys = normalize(maxBlacklistKeys, DEFAULT_MAX_BLACKLIST_KEYS);
        }

        /**
         * 创建默认本地容量配置。
         *
         * @return 默认本地容量配置
         */
        public static Local defaults() {
            return new Local(
                    DEFAULT_MAX_BUCKET_KEYS,
                    DEFAULT_MAX_REJECT_COUNTER_KEYS,
                    DEFAULT_MAX_BLACKLIST_KEYS
            );
        }

        private static int normalize(int value, int defaultValue) {
            return value > 0 ? value : defaultValue;
        }
    }
}
