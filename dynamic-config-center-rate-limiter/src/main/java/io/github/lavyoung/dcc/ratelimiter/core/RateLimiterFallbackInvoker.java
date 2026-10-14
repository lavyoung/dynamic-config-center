package io.github.lavyoung.dcc.ratelimiter.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * 限流 fallback 调用器。
 *
 * <p>当限流组件判定当前请求不能继续进入原业务方法时，AOP 层会调用本组件，
 * 根据注解中配置的 fallback 方法名，在目标对象上查找并执行兜底方法。</p>
 *
 * <p>fallback 方法约束如下：</p>
 *
 * <ul>
 *     <li>必须与原方法定义在同一个目标类或其父类中；</li>
 *     <li>方法名必须与注解中的 {@code fallbackMethod} 一致；</li>
 *     <li>参数类型必须与原方法一致；</li>
 *     <li>返回值必须由业务方保证与原方法兼容。</li>
 * </ul>
 *
 * <p>该组件不依赖 Spring，也不关心业务返回模型。它只负责反射调用并返回结果。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 0.2.0
 * @date 2026/10/14
 */
public class RateLimiterFallbackInvoker {

    public static final Logger logger = LoggerFactory.getLogger(RateLimiterFallbackInvoker.class);

    /**
     * 调用 fallback 方法。
     *
     * @param target         被拦截的目标对象
     * @param fallbackMethod fallback 方法名
     * @param parameterTypes 原方法参数类型
     * @param args           原方法实参
     * @return fallback 方法返回值
     * @throws NullPointerException     当目标对象、方法名、参数类型或实参数组为空时抛出
     * @throws IllegalArgumentException 当 fallback 方法名为空白，或找不到匹配方法时抛出
     * @throws IllegalStateException    当 fallback 方法执行失败时抛出
     */
    public Object invoke(Object target, String fallbackMethod, Class<?>[] parameterTypes, Object[] args) {
        Objects.requireNonNull(target, "target must not be null");
        String requiredFallbackMethod = requireFallbackMethod(fallbackMethod);
        Objects.requireNonNull(parameterTypes, "parameterTypes must not be null");
        Objects.requireNonNull(args, "args must not be null");

        Method method = findMethod(target.getClass(), requiredFallbackMethod, parameterTypes);
        try {
            boolean canAccess = method.canAccess(target);
            try {
                method.setAccessible(true);
                return method.invoke(target, args);
            } finally {
                method.setAccessible(canAccess);
            }
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Unable to access rate limiter fallback method: " + requiredFallbackMethod, exception);
        } catch (InvocationTargetException exception) {
            throw new IllegalStateException("Rate limiter fallback method failed: " + requiredFallbackMethod, exception.getTargetException());
        }
    }

    /**
     * 沿着类继承链查找 fallback 方法。
     *
     * @param targetClass    目标类
     * @param fallbackMethod fallback 方法名
     * @param parameterTypes 方法参数类型
     * @return 匹配到的 fallback 方法
     * @throws IllegalArgumentException 找不到匹配方法时抛出
     */
    private Method findMethod(Class<?> targetClass, String fallbackMethod, Class<?>[] parameterTypes) {
        Class<?> currentClass = targetClass;
        while (currentClass != null && currentClass != Object.class) {
            try {
                return currentClass.getDeclaredMethod(fallbackMethod, parameterTypes);
            } catch (NoSuchMethodException e) {
                currentClass = currentClass.getSuperclass();
            }
        }
        throw new IllegalArgumentException("Rate limiter fallback method not found: " + fallbackMethod);
    }

    /**
     * 校验 fallback 方法名。
     *
     * @param fallbackMethod fallback 方法名
     * @return 去除首尾空白后的方法名
     * @throws NullPointerException     当方法名为空时抛出
     * @throws IllegalArgumentException 当方法名为空白时抛出
     */
    private String requireFallbackMethod(String fallbackMethod) {
        Objects.requireNonNull(fallbackMethod, "fallbackMethod must not be null");
        String trimmed = fallbackMethod.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("fallbackMethod must not be blank");
        }
        return trimmed;
    }
}
