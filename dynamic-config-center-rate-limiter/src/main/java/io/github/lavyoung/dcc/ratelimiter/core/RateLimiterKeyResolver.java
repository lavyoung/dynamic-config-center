package io.github.lavyoung.dcc.ratelimiter.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;

/**
 * 限流 key 解析器。
 *
 * <p>该组件负责从被 AOP 拦截的方法参数中提取限流 key。
 * 例如注解配置 {@code key = "userId"} 时，解析器会尝试从方法参数中
 * 找到名为 {@code userId} 的字段、record 访问器或 JavaBean getter。</p>
 *
 * <p>解析器只负责提取 key，不负责判断限流、不调用 fallback，也不依赖 Spring。
 * 这样它可以被本地限流、Redis 限流和 starter AOP 共同复用。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 0.2.0
 * @date 2026/10/14
 */
public class RateLimiterKeyResolver {

    private static final Logger logger = LoggerFactory.getLogger(RateLimiterKeyResolver.class);
    /**
     * 从方法参数中解析限流 key。
     *
     * <p>解析顺序如下：</p>
     *
     * <ol>
     *     <li>如果参数本身是 {@link CharSequence}，直接使用该参数；</li>
     *     <li>尝试读取与 {@code keyName} 同名的 record 访问器；</li>
     *     <li>尝试读取 JavaBean getter；</li>
     *     <li>尝试读取同名字段，包括父类字段。</li>
     * </ol>
     *
     * @param keyName 注解中声明的限流字段名，例如 {@code userId}
     * @param args    被拦截方法的实参数组
     * @return 解析到的限流 key
     * @throws NullPointerException     当 {@code keyName} 或 {@code args} 为空时抛出
     * @throws IllegalArgumentException 当字段名为空白，或无法从参数中解析出有效 key 时抛出
     */
    public String resolve(String keyName, Object[] args) {
        String requiredKeyName = requireKeyName(keyName);
        Objects.requireNonNull(args, "args must not be null");

        for (Object arg : args) {
            Optional<String> resolveKey = resolveFromArgument(requiredKeyName, arg);
            if (resolveKey.isPresent()) {
                return resolveKey.get();
            }
        }
        throw new IllegalArgumentException("Unable to resolve rate limiter key: " + requiredKeyName);
    }

    /**
     * 从单个参数中解析限流 key。
     *
     * @param keyName 限流字段名
     * @param arg     方法参数
     * @return 解析到的限流 key；无法解析时返回空
     */
    private Optional<String> resolveFromArgument(String keyName, Object arg) {
        if (arg == null) {
            return Optional.empty();
        }
        if (arg instanceof CharSequence charSequence) {
            return normalize(charSequence.toString());
        }
        return resolveByAccessor(keyName, arg)
                .or(() -> resolveByGetter(keyName, arg))
                .or(() -> resolveByField(keyName, arg));
    }

    /**
     * 通过 record 访问器解析属性。
     *
     * @param keyName 属性名
     * @param arg     方法参数对象
     * @return 属性值；无法解析时返回空
     */
    private Optional<String> resolveByAccessor(String keyName, Object arg) {
        return invokeNoArgMethod(arg, keyName);
    }

    /**
     * 通过 JavaBean getter 解析属性。
     *
     * @param keyName 属性名
     * @param arg     方法参数对象
     * @return 属性值；无法解析时返回空
     */
    private Optional<String> resolveByGetter(String keyName, Object arg) {
        String getterName = "get" + Character.toUpperCase(keyName.charAt(0)) + keyName.substring(1);
        return invokeNoArgMethod(arg, getterName);
    }

    /**
     * 通过同名字段解析属性。
     *
     * @param keyName 字段名
     * @param arg     方法参数对象
     * @return 字段值；无法解析时返回空
     */
    private Optional<String> resolveByField(String keyName, Object arg) {
        Class<?> currentClass = arg.getClass();
        while (currentClass != null && currentClass != Object.class) {
            try {
                Field field = currentClass.getDeclaredField(keyName);
                boolean canAccess = field.canAccess(arg);
                try {
                    field.setAccessible(true);
                    Object value = field.get(arg);
                    return normalizeValue(value);
                } finally {
                    field.setAccessible(canAccess);
                }
            } catch (NoSuchFieldException ignored) {
                currentClass = currentClass.getSuperclass();
            } catch (IllegalAccessException ignored) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    /**
     * 调用无参方法读取属性值。
     *
     * @param arg        方法参数对象
     * @param methodName 方法名
     * @return 方法返回值；无法解析时返回空
     */
    private Optional<String> invokeNoArgMethod(Object arg, String methodName) {
        try {
            Method method = arg.getClass().getMethod(methodName);
            Object value = method.invoke(arg);
            return normalizeValue(value);
        } catch (ReflectiveOperationException e) {
            // 反射操作异常
            logger.warn("调用无参方法读取属性值失败：参数对象；{}, 参数名：{}", arg.getClass().getName(), methodName);
            return Optional.empty();
        }
    }

    /**
     * 校验限流字段名。
     *
     * @param keyName 待校验字段名
     * @return 去除首尾空白后的字段名
     * @throws NullPointerException     当字段名为空时抛出
     * @throws IllegalArgumentException 当字段名为空白时抛出
     */
    private String requireKeyName(String keyName) {
        Objects.requireNonNull(keyName, "keyName must not be null");
        String trimmedKeyName = keyName.trim();
        if (trimmedKeyName.isEmpty()) {
            throw new IllegalArgumentException("keyName must not be blank");
        }
        return trimmedKeyName;
    }

    /**
     * 将属性值转换为限流 key。
     *
     * @param value 属性值
     * @return 有效限流 key；值为空或空白时返回空
     */
    private Optional<String> normalizeValue(Object value) {
        if (value == null) {
            return Optional.empty();
        }
        return normalize(value.toString());
    }

    /**
     * 规范化字符串限流 key。
     *
     * @param value 原始字符串
     * @return 去除首尾空白后的限流 key；值为空白时返回空
     */
    private Optional<String> normalize(String value) {
        String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? Optional.empty() : Optional.of(trimmedValue);
    }

}
