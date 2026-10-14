/**
 * 限流核心协议与通用工具包。
 *
 * <p>该包定义限流组件的稳定内核，包括限流规则、判定结果、拒绝原因、存储端口、
 * key 解析器和 fallback 调用器。它表达的是“限流如何被判断和抽象”，不绑定具体
 * 存储介质，也不依赖 Spring。</p>
 *
 * <p>后续如果增加 Redis 分布式限流、网关限流或多租户限流，应优先复用本包中的
 * {@link io.github.lavyoung.dcc.ratelimiter.core.RateLimiterStore}、
 * {@link io.github.lavyoung.dcc.ratelimiter.core.RateLimiterRule} 和
 * {@link io.github.lavyoung.dcc.ratelimiter.core.RateLimiterDecision}，
 * 避免把框架接入逻辑混入核心协议。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 0.2.0
 * @date 2026/10/14
 */
package io.github.lavyoung.dcc.ratelimiter.core;
