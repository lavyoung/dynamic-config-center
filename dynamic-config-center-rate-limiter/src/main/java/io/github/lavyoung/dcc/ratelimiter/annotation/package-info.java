/**
 * 限流注解定义包。
 *
 * <p>该包用于定义业务侧可直接使用的声明式限流入口，例如
 * {@link io.github.lavyoung.dcc.ratelimiter.annotation.DccRateLimiter}。
 * 注解只描述“限流应该如何配置”，不负责执行限流判断。</p>
 *
 * <p>实际执行流程由 starter 中的 Spring AOP 切面完成，切面会读取注解参数，
 * 再委托核心限流存储进行判定。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 0.2.0
 * @date 2026/10/14
 */
package io.github.lavyoung.dcc.ratelimiter.annotation;
