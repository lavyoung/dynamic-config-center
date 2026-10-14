package io.github.lavyoung.dcc.ratelimiter.starter.switcher;

import io.github.lavyoung.dcc.core.DccValue;

/**
 * 基于 DCC 动态配置的限流运行时开关。
 *
 * <p>{@code rate-limiter.enabled} 是 DCC 配置中心里的动态 key。
 * 当配置值变更时，DCC 字段绑定器会刷新 {@link #enabled} 字段，
 * 限流切面下一次判断时即可读取到最新开关状态。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 0.2.0
 * @date 2026/10/14
 */
public class DccRateLimiterSwitch implements RateLimiterSwitch {

    /**
     * 运行时限流开关。
     *
     * <p>默认值为 {@code false}，表示组件接入后不会立刻影响业务流量。
     * 需要限流时，再通过 DCC 将该值调整为 {@code true}。</p>
     */
    @DccValue("rate-limiter.enabled:false")
    private boolean enabled;

    /**
     * 判断当前是否开启限流。
     *
     * @return {@code true} 表示执行限流判断；{@code false} 表示直接放行业务调用
     */
    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
