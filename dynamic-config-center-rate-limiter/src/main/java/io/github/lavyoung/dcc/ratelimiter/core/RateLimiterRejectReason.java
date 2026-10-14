package io.github.lavyoung.dcc.ratelimiter.core;

/**
 * 限流拒绝原因。
 *
 * <p>该枚举用于描述一次请求为什么没有被限流组件放行。
 * AOP 层可以根据拒绝原因记录不同日志，业务 fallback 也可以在后续版本中
 * 根据原因返回更精确的提示。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 0.2.0
 * @date 2026/10/14
 */
public enum RateLimiterRejectReason {

    /**
     * 没有被拒绝。
     *
     * <p>当请求允许通过时使用该原因。</p>
     */
    NONE,

    /**
     * 请求频率超过限制。
     *
     * <p>表示当前 key 没有进入黑名单，但本次请求没有拿到令牌，
     * 因此需要走 fallback。</p>
     */
    RATE_LIMITED,

    /**
     * 请求命中临时黑名单。
     *
     * <p>表示当前 key 已经因为多次超频访问进入临时黑名单，
     * 在黑名单过期前请求都会被拒绝。</p>
     */
    BLACKLISTED
}
