/**
 * 本地限流存储实现包。
 *
 * <p>该包提供基于本地内存和 Bucket4j 的令牌桶限流实现，适合课程阶段、
 * 单实例服务、开发环境验证和对全局一致性要求不高的场景。</p>
 *
 * <p>本包实现会维护本地令牌桶、拒绝计数和黑名单状态，因此必须通过容量配置控制
 * key 数量，避免高基数用户、设备或 IP 场景造成内存持续增长。</p>
 *
 * <p>集群级别的全局限流不应直接扩展本包实现，而应新增独立分布式实现包，
 * 并实现 {@link io.github.lavyoung.dcc.ratelimiter.core.RateLimiterStore}。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 0.2.0
 * @date 2026/10/14
 */
package io.github.lavyoung.dcc.ratelimiter.local;
