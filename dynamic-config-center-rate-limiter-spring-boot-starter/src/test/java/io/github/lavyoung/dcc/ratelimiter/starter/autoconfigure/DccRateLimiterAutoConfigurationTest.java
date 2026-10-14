package io.github.lavyoung.dcc.ratelimiter.starter.autoconfigure;

import io.github.lavyoung.dcc.ratelimiter.core.RateLimiterDecision;
import io.github.lavyoung.dcc.ratelimiter.core.RateLimiterFallbackInvoker;
import io.github.lavyoung.dcc.ratelimiter.core.RateLimiterKeyResolver;
import io.github.lavyoung.dcc.ratelimiter.core.RateLimiterRule;
import io.github.lavyoung.dcc.ratelimiter.core.RateLimiterStore;
import io.github.lavyoung.dcc.ratelimiter.local.LocalBucket4jRateLimiterStore;
import io.github.lavyoung.dcc.ratelimiter.starter.aop.DccRateLimiterAspect;
import io.github.lavyoung.dcc.ratelimiter.starter.switcher.RateLimiterSwitch;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link DccRateLimiterAutoConfiguration} 的自动装配测试。
 *
 * <p>本测试关注 starter 的 Spring Boot 接入边界：配置关闭时不注册限流组件，
 * 配置开启时注册默认 Bean，并且业务方提供自定义 Bean 时自动装配应主动让位。</p>
 */
class DccRateLimiterAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DccRateLimiterAutoConfiguration.class));

    /**
     * 验证未开启 starter 时不会注册限流相关 Bean。
     */
    @Test
    void shouldNotCreateRateLimiterBeansWhenDisabled() {
        contextRunner.run(context -> {
            assertFalse(context.containsBean("rateLimiterSwitch"));
            assertFalse(context.containsBean("rateLimiterStore"));
            assertFalse(context.containsBean("rateLimiterKeyResolver"));
            assertFalse(context.containsBean("rateLimiterFallbackInvoker"));
            assertFalse(context.containsBean("dccRateLimiterAspect"));
        });
    }

    /**
     * 验证开启 starter 后会注册默认限流组件。
     */
    @Test
    void shouldCreateDefaultRateLimiterBeansWhenEnabled() {
        contextRunner
                .withPropertyValues("dcc.rate-limiter.enabled=true")
                .run(context -> {
                    assertTrue(context.containsBean("rateLimiterSwitch"));
                    assertTrue(context.containsBean("rateLimiterStore"));
                    assertTrue(context.containsBean("rateLimiterKeyResolver"));
                    assertTrue(context.containsBean("rateLimiterFallbackInvoker"));
                    assertTrue(context.containsBean("dccRateLimiterAspect"));

                    assertInstanceOf(RateLimiterSwitch.class, context.getBean("rateLimiterSwitch"));
                    assertInstanceOf(LocalBucket4jRateLimiterStore.class, context.getBean(RateLimiterStore.class));
                    assertInstanceOf(RateLimiterKeyResolver.class, context.getBean(RateLimiterKeyResolver.class));
                    assertInstanceOf(RateLimiterFallbackInvoker.class, context.getBean(RateLimiterFallbackInvoker.class));
                    assertInstanceOf(DccRateLimiterAspect.class, context.getBean(DccRateLimiterAspect.class));
                });
    }

    /**
     * 验证业务方自定义限流存储时，默认本地存储不会覆盖业务实现。
     */
    @Test
    void shouldBackOffWhenCustomRateLimiterStoreExists() {
        contextRunner
                .withUserConfiguration(CustomStoreConfiguration.class)
                .withPropertyValues("dcc.rate-limiter.enabled=true")
                .run(context -> {
                    RateLimiterStore store = context.getBean(RateLimiterStore.class);

                    assertInstanceOf(AlwaysAllowRateLimiterStore.class, store);
                    assertEquals(RateLimiterDecision.allowed(), store.acquire("user-001", new RateLimiterRule(1.0, 0, 60)));
                });
    }

    /**
     * 验证配置绑定对象可以把外部配置转换为核心模块本地限流选项。
     */
    @Test
    void shouldBindPropertiesAndCreateLocalOptions() {
        contextRunner
                .withPropertyValues(
                        "dcc.rate-limiter.enabled=true",
                        "dcc.rate-limiter.fail-open-when-capacity-exceeded=false",
                        "dcc.rate-limiter.local.max-bucket-keys=10",
                        "dcc.rate-limiter.local.max-reject-counter-keys=11",
                        "dcc.rate-limiter.local.max-blacklist-keys=12"
                )
                .run(context -> {
                    DccRateLimiterProperties properties = context.getBean(DccRateLimiterProperties.class);

                    assertNotNull(properties);
                    assertTrue(properties.enabled());
                    assertFalse(properties.failOpenWhenCapacityExceeded());
                    assertEquals(10, properties.toLocalOptions().maxBucketKeys());
                    assertEquals(11, properties.toLocalOptions().maxRejectCounterKeys());
                    assertEquals(12, properties.toLocalOptions().maxBlacklistKeys());
                });
    }

    @Configuration(proxyBeanMethods = false)
    private static class CustomStoreConfiguration {

        @Bean
        RateLimiterStore customRateLimiterStore() {
            return new AlwaysAllowRateLimiterStore();
        }
    }

    private static class AlwaysAllowRateLimiterStore implements RateLimiterStore {

        @Override
        public RateLimiterDecision acquire(String key, RateLimiterRule rule) {
            return RateLimiterDecision.allowed();
        }
    }
}
