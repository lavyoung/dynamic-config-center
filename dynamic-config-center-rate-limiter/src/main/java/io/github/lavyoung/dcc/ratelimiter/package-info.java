/**
 * DCC 动态限流核心模块。
 *
 * <p>本模块提供与 Spring 无关的限流基础能力，包括限流注解定义、限流规则模型、
 * 限流判定结果、限流 key 解析、fallback 调用以及本地 Bucket4j 存储实现。</p>
 *
 * <p>该模块刻意不依赖 Spring Boot、AOP 或具体 Web 框架，目的是让限流能力可以被
 * starter、普通 Java 应用或未来的分布式限流实现共同复用。</p>
 *
 * <p>包职责划分：</p>
 * <ul>
 *     <li>{@code annotation}: 面向业务方法暴露的限流注解。</li>
 *     <li>{@code core}: 限流协议、规则、判定结果、key 解析和 fallback 调用。</li>
 *     <li>{@code local}: 基于本地内存和 Bucket4j 的限流存储实现。</li>
 * </ul>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 0.2.0
 * @date 2026/10/14
 */
package io.github.lavyoung.dcc.ratelimiter;
