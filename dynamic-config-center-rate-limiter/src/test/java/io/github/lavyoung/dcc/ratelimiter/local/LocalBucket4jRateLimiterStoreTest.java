package io.github.lavyoung.dcc.ratelimiter.local;

import io.github.lavyoung.dcc.ratelimiter.core.RateLimiterDecision;
import io.github.lavyoung.dcc.ratelimiter.core.RateLimiterRejectReason;
import io.github.lavyoung.dcc.ratelimiter.core.RateLimiterRule;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link LocalBucket4jRateLimiterStore} 单元测试。
 *
 * <p>本测试类验证本地 Bucket4j 限流实现的核心行为，包括正常放行、
 * 超频拒绝、连续超频进入临时黑名单、限流 key 清洗以及容量上限策略。
 * 这些用例保障 starter AOP 后续只需要依赖 {@link io.github.lavyoung.dcc.ratelimiter.core.RateLimiterStore}
 * 端口即可完成稳定拦截。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/14
 */
class LocalBucket4jRateLimiterStoreTest {

    private static final String USER_ID = "user-001";

    /**
     * 验证第一次访问可以消费令牌并正常放行。
     *
     * <p>目的：确保本地限流器在新 key 首次进入时会创建令牌桶，
     * 并返回允许通过的判定结果。</p>
     */
    @Test
    void shouldAllowFirstRequestWhenTokenAvailable() {
        // Given
        LocalBucket4jRateLimiterStore store = new LocalBucket4jRateLimiterStore();
        RateLimiterRule rule = new RateLimiterRule(1.0D, 0, 60L);

        // When
        RateLimiterDecision decision = store.acquire(USER_ID, rule);

        // Then
        assertAll(
                () -> assertTrue(decision.allow()),
                () -> assertEquals(RateLimiterRejectReason.NONE, decision.reason())
        );
    }

    /**
     * 验证同一 key 在令牌不足时会被限流拒绝。
     *
     * <p>目的：确保本地令牌桶不会让超频请求进入业务链路。</p>
     */
    @Test
    void shouldRejectRequestWhenTokenUnavailable() {
        // Given
        LocalBucket4jRateLimiterStore store = new LocalBucket4jRateLimiterStore();
        RateLimiterRule rule = new RateLimiterRule(1.0D, 0, 60L);
        store.acquire(USER_ID, rule);

        // When
        RateLimiterDecision decision = store.acquire(USER_ID, rule);

        // Then
        assertAll(
                () -> assertFalse(decision.allow()),
                () -> assertEquals(RateLimiterRejectReason.RATE_LIMITED, decision.reason())
        );
    }

    /**
     * 验证连续超频超过阈值后会进入临时黑名单。
     *
     * <p>目的：保障高频重复访问不会只是反复限流，还会在达到阈值后
     * 进入更强的临时黑名单保护状态。</p>
     */
    @Test
    void shouldBlacklistKeyWhenRejectCountExceedsThreshold() {
        // Given
        LocalBucket4jRateLimiterStore store = new LocalBucket4jRateLimiterStore();
        RateLimiterRule rule = new RateLimiterRule(1.0D, 1, 60L);
        store.acquire(USER_ID, rule);
        store.acquire(USER_ID, rule);
        store.acquire(USER_ID, rule);

        // When
        RateLimiterDecision decision = store.acquire(USER_ID, rule);

        // Then
        assertAll(
                () -> assertFalse(decision.allow()),
                () -> assertEquals(RateLimiterRejectReason.BLACKLISTED, decision.reason())
        );
    }

    /**
     * 验证限流 key 会先去除首尾空白再参与状态判断。
     *
     * <p>目的：避免调用方传入带空格的用户标识时绕过同一个用户的限流状态。</p>
     */
    @Test
    void shouldTrimKeyBeforeUsingLimiterState() {
        // Given
        LocalBucket4jRateLimiterStore store = new LocalBucket4jRateLimiterStore();
        RateLimiterRule rule = new RateLimiterRule(1.0D, 0, 60L);
        store.acquire(USER_ID, rule);

        // When
        RateLimiterDecision decision = store.acquire("  " + USER_ID + "  ", rule);

        // Then
        assertAll(
                () -> assertFalse(decision.allow()),
                () -> assertEquals(RateLimiterRejectReason.RATE_LIMITED, decision.reason())
        );
    }

    /**
     * 验证容量达到上限且配置为 fail-open 时，新 key 会被放行。
     *
     * <p>目的：保障限流组件自身状态容量达到上限时，默认不误伤业务请求。</p>
     */
    @Test
    void shouldAllowNewKeyWhenCapacityExceededAndFailOpen() {
        // Given
        LocalRateLimiterOptions options = new LocalRateLimiterOptions(1, 1, 1, true);
        LocalBucket4jRateLimiterStore store = new LocalBucket4jRateLimiterStore(options);
        RateLimiterRule rule = new RateLimiterRule(1.0D, 0, 60L);
        store.acquire(USER_ID, rule);

        // When
        RateLimiterDecision decision = store.acquire("user-002", rule);

        // Then
        assertAll(
                () -> assertTrue(decision.allow()),
                () -> assertEquals(RateLimiterRejectReason.NONE, decision.reason())
        );
    }

    /**
     * 验证容量达到上限且配置为 fail-closed 时，新 key 会被限流拒绝。
     *
     * <p>目的：允许业务在保护系统优先的场景中选择更保守的容量溢出策略。</p>
     */
    @Test
    void shouldRejectNewKeyWhenCapacityExceededAndFailClosed() {
        // Given
        LocalRateLimiterOptions options = new LocalRateLimiterOptions(1, 1, 1, false);
        LocalBucket4jRateLimiterStore store = new LocalBucket4jRateLimiterStore(options);
        RateLimiterRule rule = new RateLimiterRule(1.0D, 0, 60L);
        store.acquire(USER_ID, rule);

        // When
        RateLimiterDecision decision = store.acquire("user-002", rule);

        // Then
        assertAll(
                () -> assertFalse(decision.allow()),
                () -> assertEquals(RateLimiterRejectReason.RATE_LIMITED, decision.reason())
        );
    }

    /**
     * 验证空白 key 会被拒绝。
     *
     * <p>目的：避免所有非法调用共享一个无意义的空 key，污染限流状态。</p>
     */
    @Test
    void shouldThrowExceptionWhenKeyIsBlank() {
        // Given
        LocalBucket4jRateLimiterStore store = new LocalBucket4jRateLimiterStore();
        RateLimiterRule rule = new RateLimiterRule(1.0D, 0, 60L);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> store.acquire("   ", rule));
    }
}
