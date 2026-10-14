/**
 * DCC 限流组件的 Spring Boot Starter。
 *
 * <p>本模块负责把 {@code dynamic-config-center-rate-limiter} 中的纯限流能力
 * 接入 Spring Boot 应用，包括自动装配、运行时动态开关、AOP 拦截和 fallback 调用。</p>
 *
 * <p>包职责划分：</p>
 * <ul>
 *     <li>{@code autoconfigure}: Spring Boot 自动装配与配置属性。</li>
 *     <li>{@code switcher}: 限流运行时开关抽象及 DCC 实现。</li>
 *     <li>{@code aop}: {@code @DccRateLimiter} 注解的 Spring AOP 拦截实现。</li>
 * </ul>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 0.2.0
 * @date 2026/10/14
 */
package io.github.lavyoung.dcc.ratelimiter.starter;