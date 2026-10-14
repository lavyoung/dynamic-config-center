package io.github.lavyoung.dcc.ratelimiter.core;

/**
 * 限流状态存储端口。
 *
 * <p>该接口用于屏蔽不同限流状态存储方案之间的差异。
 * 第一版可以使用本地内存实现，后续版本可以使用 Redis 实现，
 * AOP 拦截层始终只依赖该接口完成限流判定。</p>
 *
 * <p>实现类需要负责维护以下状态：</p>
 *
 * <ul>
 *     <li>指定 key 的令牌桶或访问窗口；</li>
 *     <li>指定 key 的连续限流次数；</li>
 *     <li>指定 key 的临时黑名单状态及过期时间。</li>
 * </ul>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 0.2.0
 * @date 2026/10/14
 */
public interface RateLimiterStore {

    /**
     * 尝试获取一次访问许可。
     *
     * <p>当返回 {@link RateLimiterDecision#allowed()} 时，调用方应继续执行原方法。
     * 当返回拒绝结果时，调用方应执行 fallback 方法，不应继续进入原业务链路。</p>
     *
     * @param key  限流 key，例如用户 ID、IP、活动 ID
     * @param rule 限流规则
     * @return 本次访问的限流判定结果
     * @throws NullPointerException     当 {@code key} 或 {@code rule} 为空时抛出
     * @throws IllegalArgumentException 当限流 key 为空白时抛出
     */
    RateLimiterDecision acquire(String key, RateLimiterRule rule);
}
