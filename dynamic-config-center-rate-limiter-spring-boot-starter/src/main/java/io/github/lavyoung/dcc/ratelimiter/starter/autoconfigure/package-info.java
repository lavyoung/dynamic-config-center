/**
 * DCC 限流 starter 自动装配包。
 *
 * <p>该包负责把限流核心模块注册到 Spring Boot 容器中，包括配置属性绑定、
 * 默认本地限流存储、key 解析器、fallback 调用器、运行时开关和 AOP 切面。</p>
 *
 * <p>本包只负责“装配默认能力”，不承载具体限流算法。业务方可以通过声明自己的
 * {@link io.github.lavyoung.dcc.ratelimiter.core.RateLimiterStore} Bean
 * 替换默认本地实现。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 0.2.0
 * @date 2026/10/14
 */
package io.github.lavyoung.dcc.ratelimiter.starter.autoconfigure;
