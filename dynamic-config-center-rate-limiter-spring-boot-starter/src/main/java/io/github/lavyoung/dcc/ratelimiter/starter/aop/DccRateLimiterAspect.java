package io.github.lavyoung.dcc.ratelimiter.starter.aop;

import io.github.lavyoung.dcc.ratelimiter.annotation.DccRateLimiter;
import io.github.lavyoung.dcc.ratelimiter.core.RateLimiterDecision;
import io.github.lavyoung.dcc.ratelimiter.core.RateLimiterFallbackInvoker;
import io.github.lavyoung.dcc.ratelimiter.core.RateLimiterKeyResolver;
import io.github.lavyoung.dcc.ratelimiter.core.RateLimiterRule;
import io.github.lavyoung.dcc.ratelimiter.core.RateLimiterStore;
import io.github.lavyoung.dcc.ratelimiter.starter.switcher.RateLimiterSwitch;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

/**
 * {@link DccRateLimiter} 注解的 Spring AOP 拦截器。
 *
 * <p>该切面是限流 starter 的核心入口。它不直接维护限流数据，
 * 也不关心底层限流算法，而是负责把 Spring 方法调用转换成
 * 核心限流模块可以理解的 {@link RateLimiterRule} 和限流 key。</p>
 *
 * <p>执行流程如下：</p>
 * <ol>
 *     <li>读取 DCC 运行时开关，未开启时直接放行业务方法。</li>
 *     <li>从方法参数中解析限流 key。</li>
 *     <li>根据注解参数构建限流规则。</li>
 *     <li>调用 {@link RateLimiterStore} 判断当前请求是否允许通过。</li>
 *     <li>允许通过则执行业务方法；不允许通过则调用 fallback 方法。</li>
 * </ol>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 0.2.0
 * @date 2026/10/14
 */
@Aspect
public class DccRateLimiterAspect {

    private final RateLimiterSwitch rateLimiterSwitch;
    private final RateLimiterStore rateLimiterStore;
    private final RateLimiterKeyResolver keyResolver;
    private final RateLimiterFallbackInvoker fallbackInvoker;

    /**
     * 创建限流切面。
     *
     * @param rateLimiterSwitch 限流运行时开关
     * @param rateLimiterStore  限流状态存储
     * @param keyResolver       限流 key 解析器
     * @param fallbackInvoker   fallback 方法调用器
     */
    public DccRateLimiterAspect(RateLimiterSwitch rateLimiterSwitch, RateLimiterStore rateLimiterStore, RateLimiterKeyResolver keyResolver, RateLimiterFallbackInvoker fallbackInvoker) {
        this.rateLimiterSwitch = rateLimiterSwitch;
        this.rateLimiterStore = rateLimiterStore;
        this.keyResolver = keyResolver;
        this.fallbackInvoker = fallbackInvoker;
    }

    /**
     * 拦截被 {@link DccRateLimiter} 标记的方法。
     *
     * <p>当动态限流开关关闭时，本方法会直接放行业务调用，保证组件接入后
     * 默认不影响业务。当开关开启时，会基于注解中的 key、QPS、黑名单阈值等
     * 参数执行限流判断。</p>
     *
     * @param joinPoint      Spring AOP 方法调用上下文
     * @param dccRateLimiter 方法上的限流注解
     * @return 原业务方法返回值，或 fallback 方法返回值
     * @throws Throwable 原业务方法执行异常，或 fallback 调用异常
     */
    @Around("@annotation(dccRateLimiter)")
    public Object around(ProceedingJoinPoint joinPoint, DccRateLimiter dccRateLimiter) throws Throwable {
        if (!rateLimiterSwitch.isEnabled()) {
            return joinPoint.proceed();
        }
        String key = keyResolver.resolve(dccRateLimiter.key(), joinPoint.getArgs());
        RateLimiterRule rateLimiterRule = new RateLimiterRule(
                dccRateLimiter.permitsPerSecond(),
                dccRateLimiter.blacklistCount(),
                dccRateLimiter.blacklistTtlSeconds()
        );
        RateLimiterDecision decision = rateLimiterStore.acquire(key, rateLimiterRule);
        if (decision.allow()) {
            return joinPoint.proceed();
        }
        // 从 MethodSignature 里取原方法参数类型。
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        return fallbackInvoker.invoke(
                joinPoint.getTarget(),
                dccRateLimiter.fallbackMethod(),
                methodSignature.getParameterTypes(),
                joinPoint.getArgs()
        );
    }
}
