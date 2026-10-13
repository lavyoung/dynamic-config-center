package io.github.lavyoung.dcc.core;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记由动态配置中心维护的字段。
 *
 * <p>值使用 {@code key:defaultValue} 格式。组件启动时使用默认值初始化字段，
 * 配置中心发生变化后再自动更新字段。
 *
 * @author lavyoung
 * @since 0.1.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface DccValue {

    /**
     * 获取动态配置键和默认值定义。
     *
     * @return {@code key:defaultValue} 格式的配置定义
     */
    String value();
}
