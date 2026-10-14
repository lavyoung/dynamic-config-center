package io.github.lavyoung.dcc.ratelimiter.starter.aop;

import io.github.lavyoung.dcc.ratelimiter.annotation.DccRateLimiter;
import io.github.lavyoung.dcc.ratelimiter.core.RateLimiterDecision;
import io.github.lavyoung.dcc.ratelimiter.core.RateLimiterFallbackInvoker;
import io.github.lavyoung.dcc.ratelimiter.core.RateLimiterKeyResolver;
import io.github.lavyoung.dcc.ratelimiter.core.RateLimiterRule;
import io.github.lavyoung.dcc.ratelimiter.core.RateLimiterStore;
import io.github.lavyoung.dcc.ratelimiter.starter.switcher.RateLimiterSwitch;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link DccRateLimiterAspect} 的 AOP 行为测试。
 *
 * <p>本测试不关注 Spring Boot 自动装配，而是直接验证 {@link DccRateLimiter}
 * 注解被切面拦截后的核心分支：动态开关关闭、限流放行、限流拒绝、
 * 黑名单拒绝以及 fallback 配置错误。</p>
 */
class DccRateLimiterAspectTest {

    /**
     * 验证动态限流开关关闭时，切面不调用限流存储，直接执行业务方法。
     */
    @Test
    void shouldProceedWithoutRateLimiterStoreWhenSwitchDisabled() {
        CountingRateLimiterStore store = new CountingRateLimiterStore(RateLimiterDecision.rateLimited());
        DemoService proxy = createProxy(new DemoService(), () -> false, store);

        String result = proxy.query(new QueryRequest("user-001"));

        assertEquals("business:user-001", result);
        assertEquals(0, store.acquireCount());
    }

    /**
     * 验证动态限流开关开启且限流存储放行时，切面返回原业务方法结果。
     */
    @Test
    void shouldProceedWhenRateLimiterAllowsRequest() {
        CountingRateLimiterStore store = new CountingRateLimiterStore(RateLimiterDecision.allowed());
        DemoService proxy = createProxy(new DemoService(), () -> true, store);

        String result = proxy.query(new QueryRequest("user-002"));

        assertEquals("business:user-002", result);
        assertEquals(1, store.acquireCount());
        assertEquals("user-002", store.lastKey());
        assertEquals(new RateLimiterRule(1.0, 3, 60), store.lastRule());
    }

    /**
     * 验证动态限流开关开启且限流存储拒绝时，切面调用 fallback 方法。
     */
    @Test
    void shouldInvokeFallbackWhenRateLimited() {
        CountingRateLimiterStore store = new CountingRateLimiterStore(RateLimiterDecision.rateLimited());
        DemoService proxy = createProxy(new DemoService(), () -> true, store);

        String result = proxy.query(new QueryRequest("user-003"));

        assertEquals("fallback:user-003", result);
        assertEquals(1, store.acquireCount());
    }

    /**
     * 验证黑名单拒绝时，切面同样调用 fallback 方法。
     */
    @Test
    void shouldInvokeFallbackWhenBlacklisted() {
        CountingRateLimiterStore store = new CountingRateLimiterStore(RateLimiterDecision.blacklisted());
        DemoService proxy = createProxy(new DemoService(), () -> true, store);

        String result = proxy.query(new QueryRequest("user-004"));

        assertEquals("fallback:user-004", result);
        assertEquals(1, store.acquireCount());
    }

    /**
     * 验证 fallback 方法缺失时抛出明确异常，避免限流后静默失败。
     */
    @Test
    void shouldThrowExceptionWhenFallbackMethodMissing() {
        CountingRateLimiterStore store = new CountingRateLimiterStore(RateLimiterDecision.rateLimited());
        BrokenFallbackService proxy = createProxy(new BrokenFallbackService(), () -> true, store);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> proxy.query(new QueryRequest("user-005"))
        );

        assertTrue(exception.getMessage().contains("Rate limiter fallback method not found"));
    }

    private static <T> T createProxy(T target, RateLimiterSwitch rateLimiterSwitch, RateLimiterStore store) {
        DccRateLimiterAspect aspect = new DccRateLimiterAspect(
                rateLimiterSwitch,
                store,
                new RateLimiterKeyResolver(),
                new RateLimiterFallbackInvoker()
        );
        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(target);
        proxyFactory.addAspect(aspect);
        return proxyFactory.getProxy();
    }

    private record QueryRequest(String userId) {
    }

    public static class DemoService {

        public DemoService() {
        }

        @DccRateLimiter(
                key = "userId",
                fallbackMethod = "queryFallback",
                permitsPerSecond = 1.0,
                blacklistCount = 3,
                blacklistTtlSeconds = 60
        )
        public String query(QueryRequest request) {
            return "business:" + request.userId();
        }

        private String queryFallback(QueryRequest request) {
            return "fallback:" + request.userId();
        }
    }

    public static class BrokenFallbackService {

        public BrokenFallbackService() {
        }

        @DccRateLimiter(
                key = "userId",
                fallbackMethod = "missingFallback",
                permitsPerSecond = 1.0,
                blacklistCount = 3,
                blacklistTtlSeconds = 60
        )
        public String query(QueryRequest request) {
            return "business:" + request.userId();
        }
    }

    private static class CountingRateLimiterStore implements RateLimiterStore {

        private final RateLimiterDecision decision;
        private int acquireCount;
        private String lastKey;
        private RateLimiterRule lastRule;

        private CountingRateLimiterStore(RateLimiterDecision decision) {
            this.decision = decision;
        }

        @Override
        public RateLimiterDecision acquire(String key, RateLimiterRule rule) {
            acquireCount++;
            lastKey = key;
            lastRule = rule;
            return decision;
        }

        private int acquireCount() {
            return acquireCount;
        }

        private String lastKey() {
            return lastKey;
        }

        private RateLimiterRule lastRule() {
            return lastRule;
        }
    }
}
