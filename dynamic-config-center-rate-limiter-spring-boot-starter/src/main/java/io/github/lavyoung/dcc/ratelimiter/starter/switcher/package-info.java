/**
 * 限流运行时开关包。
 *
 * <p>该包用于隔离“限流是否开启”的判断来源。AOP 切面只依赖
 * {@link io.github.lavyoung.dcc.ratelimiter.starter.switcher.RateLimiterSwitch}
 * 抽象，不直接依赖 DCC 字段绑定细节。</p>
 *
 * <p>默认实现通过 {@code @DccValue} 读取 DCC 动态配置。未来如果需要接入其他
 * 配置中心，只需要新增 {@link io.github.lavyoung.dcc.ratelimiter.starter.switcher.RateLimiterSwitch}
 * 实现即可。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 0.2.0
 * @date 2026/10/14
 */
package io.github.lavyoung.dcc.ratelimiter.starter.switcher;
