package io.github.lavyoung.dcc.ratelimiter.starter.autoconfigure;

import io.github.lavyoung.dcc.ratelimiter.core.RateLimiterFallbackInvoker;
import io.github.lavyoung.dcc.ratelimiter.core.RateLimiterKeyResolver;
import io.github.lavyoung.dcc.ratelimiter.core.RateLimiterStore;
import io.github.lavyoung.dcc.ratelimiter.local.LocalBucket4jRateLimiterStore;
import io.github.lavyoung.dcc.ratelimiter.starter.aop.DccRateLimiterAspect;
import io.github.lavyoung.dcc.ratelimiter.starter.switcher.DccRateLimiterSwitch;
import io.github.lavyoung.dcc.ratelimiter.starter.switcher.RateLimiterSwitch;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * DCC 限流组件的 Spring Boot 自动装配。
 *
 * <p>该类负责把纯限流核心模块装配到 Spring Boot 应用中。业务项目引入
 * starter 后，只需要开启 {@code dcc.rate-limiter.enabled=true}，
 * 即可使用 {@code @DccRateLimiter} 注解完成动态限流。</p>
 *
 * <p>默认实现使用本地 Bucket4j 令牌桶。后续如果需要 Redis 分布式限流，
 * 可以由业务方或新模块提供自定义 {@link RateLimiterStore} Bean，
 * 自动装配会因为 {@link ConditionalOnMissingBean} 而让位。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 0.2.0
 * @date 2026/10/14
 */
@AutoConfiguration
@EnableConfigurationProperties(DccRateLimiterProperties.class)
@ConditionalOnProperty(prefix = "dcc.rate-limiter", name = "enabled", havingValue = "true")
public class DccRateLimiterAutoConfiguration {

    /**
     * 创建限流运行时开关。
     *
     * <p>默认实现基于 {@code @DccValue} 绑定 DCC 配置中心中的
     * {@code rate-limiter.enabled} 动态配置。</p>
     *
     * @return 限流运行时开关
     */
    @Bean
    @ConditionalOnMissingBean
    public RateLimiterSwitch rateLimiterSwitch() {
        return new DccRateLimiterSwitch();
    }

    /**
     * 创建本地限流存储。
     *
     * <p>该默认实现使用 Bucket4j 本地令牌桶，适合课程阶段和单机应用。
     * 如果应用需要集群共享限流状态，应替换为 Redis、Hazelcast 或其他
     * 分布式实现。</p>
     *
     * @param properties 限流 starter 启动配置
     * @return 限流状态存储
     */
    @Bean
    @ConditionalOnMissingBean
    public RateLimiterStore rateLimiterStore(DccRateLimiterProperties properties) {
        return new LocalBucket4jRateLimiterStore(properties.toLocalOptions());
    }

    /**
     * 创建限流 key 解析器。
     *
     * @return 限流 key 解析器
     */
    @Bean
    @ConditionalOnMissingBean
    public RateLimiterKeyResolver rateLimiterKeyResolver() {
        return new RateLimiterKeyResolver();
    }

    /**
     * 创建 fallback 调用器。
     *
     * @return fallback 调用器
     */
    @Bean
    @ConditionalOnMissingBean
    public RateLimiterFallbackInvoker rateLimiterFallbackInvoker() {
        return new RateLimiterFallbackInvoker();
    }

    /**
     * 创建 {@code @DccRateLimiter} 注解切面。
     *
     * @param rateLimiterSwitch 限流运行时开关
     * @param rateLimiterStore  限流状态存储
     * @param keyResolver       限流 key 解析器
     * @param fallbackInvoker   fallback 调用器
     * @return 限流注解切面
     */
    @Bean
    @ConditionalOnMissingBean
    public DccRateLimiterAspect dccRateLimiterAspect(RateLimiterSwitch rateLimiterSwitch,
                                                     RateLimiterStore rateLimiterStore,
                                                     RateLimiterKeyResolver keyResolver,
                                                     RateLimiterFallbackInvoker fallbackInvoker) {
        return new DccRateLimiterAspect(
                rateLimiterSwitch,
                rateLimiterStore,
                keyResolver,
                fallbackInvoker
        );
    }
}
