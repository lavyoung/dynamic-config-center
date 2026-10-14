package io.github.lavyoung.dcc.ratelimiter.starter.switcher;

/**
 * 限流组件的运行时开关。
 *
 * <p>该接口隔离“限流切面”和“动态配置来源”。切面只关心当前是否开启限流，
 * 不关心开关值来自 DCC、配置中心还是其他实现。</p>
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 0.2.0
 * @date 2026/10/14
 */
public interface RateLimiterSwitch {

    /**
     * 判断当前是否开启限流。
     *
     * @return {@code true} 表示执行限流判断；{@code false} 表示直接放行业务调用
     */
    boolean isEnabled();
}
