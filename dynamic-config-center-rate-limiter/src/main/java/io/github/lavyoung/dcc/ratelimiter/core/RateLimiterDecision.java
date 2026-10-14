package io.github.lavyoung.dcc.ratelimiter.core;

import java.util.Objects;

/**
 * 限流判定结果。
 *
 * <p>该 record 表示一次限流检查的结果。限流存储实现只负责返回该模型，
 * 不直接调用业务 fallback，也不依赖 Spring AOP。这样本地限流、Redis 限流、
 * 其他分布式限流实现都可以复用同一套返回协议。</p>
 *
 * @param allow  是否允许请求继续执行
 * @param reason 拒绝原因；允许通过时为 {@link RateLimiterRejectReason#NONE}
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 0.2.0
 * @date 2026/10/14
 */
public record RateLimiterDecision(
        boolean allow,
        RateLimiterRejectReason reason
) {

    /**
     * 创建允许通过的判定结果。
     *
     * @return 允许通过的判定结果
     */
    public static RateLimiterDecision allowed() {
        return new RateLimiterDecision(true, RateLimiterRejectReason.NONE);
    }

    /**
     * 创建请求频率超过限制的判定结果。
     *
     * @return 限流拒绝的判定结果
     */
    public static RateLimiterDecision rateLimited() {
        return new RateLimiterDecision(false, RateLimiterRejectReason.RATE_LIMITED);
    }

    /**
     * 创建命中临时黑名单的判定结果。
     *
     * @return 黑名单拒绝的判定结果
     */
    public static RateLimiterDecision blacklisted() {
        return new RateLimiterDecision(false, RateLimiterRejectReason.BLACKLISTED);
    }

    /**
     * 校验判定结果的基本不变量。
     *
     * <p>允许通过时原因必须是 {@link RateLimiterRejectReason#NONE}；
     * 拒绝通过时原因不能是 {@link RateLimiterRejectReason#NONE}。</p>
     *
     * @throws NullPointerException     当拒绝原因为空时抛出
     * @throws IllegalArgumentException 当允许状态和拒绝原因不匹配时抛出
     */
    public RateLimiterDecision {
        Objects.requireNonNull(reason, "reason must not be null");
        if (allow && reason != RateLimiterRejectReason.NONE) {
            throw new IllegalArgumentException("allowed decision must use NONE reason");
        }
        if (!allow && reason == RateLimiterRejectReason.NONE) {
            throw new IllegalArgumentException("rejected decision must not use NONE reason");
        }
    }
}
