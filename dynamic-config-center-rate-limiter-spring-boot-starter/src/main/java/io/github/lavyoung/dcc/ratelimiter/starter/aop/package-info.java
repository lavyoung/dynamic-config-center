/**
 * DCC 限流注解的 Spring AOP 接入包。
 *
 * <p>该包负责拦截被 {@link io.github.lavyoung.dcc.ratelimiter.annotation.DccRateLimiter}
 * 标记的业务方法，并完成运行时开关判断、限流 key 解析、限流规则构建、
 * 限流存储判定和 fallback 调用。</p>
 *
 * <p>切面只做方法调用编排，不直接维护限流状态。限流状态统一委托
 * {@link io.github.lavyoung.dcc.ratelimiter.core.RateLimiterStore} 处理。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 0.2.0
 * @date 2026/10/14
 */
package io.github.lavyoung.dcc.ratelimiter.starter.aop;
