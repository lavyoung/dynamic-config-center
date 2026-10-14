package io.github.lavyoung.dcc.ratelimiter.annotation;

import java.lang.annotation.*;

/**
 * 动态配置限流注解。
 *
 * <p>该注解用于标记需要被限流保护的方法。组件会在 Spring AOP
 * 拦截阶段读取注解配置，根据指定的限流 key 提取请求标识，并结合 DCC
 * 动态开关决定是否执行限流判断。</p>
 *
 * <p>第一版实现使用本地内存限流，适合单机开发、课程验证和小流量场景。
 * 后续版本可以在不改变注解使用方式的前提下，将限流状态存储替换为 Redis，
 * 从而支持多实例共享限流状态。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 0.2.0
 * @date 2026/10/14
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DccRateLimiter {

    /**
     * 限流 key 字段名。
     *
     * <p>组件会从被拦截方法的入参中提取该字段值作为限流维度。
     * 例如抽奖接口通常按 {@code userId} 限流。</p>
     *
     * @return 限流字段名
     */
    String key();

    /**
     * 被限流或命中临时黑名单后的回调方法名。
     *
     * <p>回调方法需要与原方法定义在同一个类中，并保持相同入参和返回值。
     * 这样 AOP 可以在拒绝访问时调用业务方自定义的兜底响应。</p>
     *
     * @return fallback 方法名
     */
    String fallbackMethod();

    /**
     * 每秒允许通过的请求数。
     *
     * <p>第一版本地实现会使用令牌桶语义处理该值。</p>
     *
     * @return 每秒许可数
     */
    double permitsPerSecond() default 1.00;

    /**
     * 临时黑名单触发阈值。
     *
     * <p>当同一个限流 key 连续被限流的次数超过该阈值后，
     * 组件会将该 key 放入临时黑名单。值为 {@code 0} 时表示不启用黑名单。</p>
     *
     * @return 黑名单触发阈值
     */
    int blacklistCount() default 0;

    /**
     * 临时黑名单过期时间，单位秒。
     *
     * <p>第一版本地实现会使用该值控制本地黑名单缓存过期时间；
     * 后续 Redis 实现可复用该值作为 Redis key TTL。</p>
     *
     * @return 黑名单过期秒数
     */
    long blacklistTtlSeconds() default 86_400L;
}
