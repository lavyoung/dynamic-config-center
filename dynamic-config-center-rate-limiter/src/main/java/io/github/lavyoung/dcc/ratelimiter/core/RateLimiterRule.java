package io.github.lavyoung.dcc.ratelimiter.core;

/**
 * 限流规则。
 *
 * <p>该 record 用于封装一次限流判定所需的规则参数。
 * 相比在方法参数中平铺 {@code permitsPerSecond}、{@code blacklistCount}
 * 和 {@code blacklistTtlSeconds}，使用规则对象可以让接口更稳定，
 * 也便于后续扩展 Redis 全局限流、滑动窗口限流、单机/全局模式等能力。</p>
 *
 * @param permitsPerSecond    每秒允许通过的请求数
 * @param blacklistCount      进入临时黑名单前允许的限流失败次数；为 {@code 0} 时表示不启用黑名单
 * @param blacklistTtlSeconds 临时黑名单过期时间，单位秒
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 0.2.0
 * @date 2026/10/14
 */
public record RateLimiterRule(
        double permitsPerSecond,
        int blacklistCount,
        long blacklistTtlSeconds
) {

    /**
     * 校验限流规则参数。
     *
     * @throws IllegalArgumentException 当每秒许可数不是正数、黑名单阈值为负数、
     *                                  或黑名单过期时间不是正数时抛出
     */
    public RateLimiterRule {
        if (permitsPerSecond <= 0) {
            throw new IllegalArgumentException("permitsPerSecond must be positive");
        }
        if (blacklistCount < 0) {
            throw new IllegalArgumentException("blacklistCount must not be negative");
        }
        if (blacklistTtlSeconds <= 0) {
            throw new IllegalArgumentException("blacklistTtlSeconds must be positive");
        }
    }
}
