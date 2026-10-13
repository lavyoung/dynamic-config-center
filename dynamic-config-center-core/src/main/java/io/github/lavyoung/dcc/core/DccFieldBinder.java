package io.github.lavyoung.dcc.core;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态配置字段绑定器。
 *
 * <p>负责扫描 {@link DccValue} 字段、写入默认值、注册配置监听并完成基础类型转换。
 * 组件不关心业务对象的具体含义，因此可以被任何 Spring Boot 项目复用。
 *
 * @author lavyoung
 * @since 0.1.0
 */
public final class DccFieldBinder implements AutoCloseable {

    private final DccConfigSource configSource;
    private final Map<String, Object> registeredTargets = new ConcurrentHashMap<>();
    private final List<AutoCloseable> registrations = new ArrayList<>();

    /**
     * 创建字段绑定器。
     *
     * @param configSource 动态配置数据源
     * @throws NullPointerException 数据源为空时抛出
     */
    public DccFieldBinder(DccConfigSource configSource) {
        this.configSource = Objects.requireNonNull(configSource, "configSource must not be null");
    }

    /**
     * 扫描目标对象并注册全部动态配置字段。
     *
     * @param target 待绑定对象
     * @throws Exception 配置监听注册失败时抛出
     */
    public synchronized void bind(Object target) throws Exception {
        Objects.requireNonNull(target, "target must not be null");
        for (Field field : target.getClass().getDeclaredFields()) {
            DccValue annotation = field.getAnnotation(DccValue.class);
            if (annotation == null) {
                continue;
            }
            Property property = parse(annotation.value());
            Object previous = registeredTargets.putIfAbsent(property.key(), target);
            if (previous != null && previous != target) {
                throw new IllegalArgumentException("Duplicate DCC key: " + property.key());
            }
            setField(target, field, property.defaultValue());
            registrations.add(configSource.watch(
                    property.key(),
                    value -> setField(target, field, value)
            ));
        }
    }

    private static Property parse(String definition) {
        Objects.requireNonNull(definition, "DCC definition must not be null");
        String[] parts = definition.split(":", 2);
        if (parts.length != 2 || parts[0].isBlank()) {
            throw new IllegalArgumentException("DCC definition must use key:defaultValue format");
        }
        return new Property(parts[0].trim(), parts[1].trim());
    }

    private static void setField(Object target, Field field, String rawValue) {
        boolean accessible = field.canAccess(target);
        try {
            field.setAccessible(true);
            field.set(target, convert(field.getType(), rawValue));
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Unable to update DCC field: " + field.getName(), exception);
        } finally {
            field.setAccessible(accessible);
        }
    }

    private static Object convert(Class<?> type, String value) {
        if (type == String.class) return value;
        if (type == boolean.class || type == Boolean.class) {
            if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                throw new IllegalArgumentException("Invalid boolean DCC value: " + value);
            }
            return Boolean.parseBoolean(value);
        }
        try {
            if (type == int.class || type == Integer.class) return Integer.parseInt(value);
            if (type == long.class || type == Long.class) return Long.parseLong(value);
            if (type == double.class || type == Double.class) return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid DCC value: " + value, exception);
        }
        throw new IllegalArgumentException("Unsupported DCC field type: " + type.getName());
    }

    /**
     * 关闭所有配置监听注册。
     */
    @Override
    public synchronized void close() {
        registrations.forEach(registration -> {
            try {
                registration.close();
            } catch (Exception ignored) {
                // 关闭阶段继续释放其他注册，避免单个监听器阻塞容器销毁。
            }
        });
        registrations.clear();
        registeredTargets.clear();
    }

    private record Property(String key, String defaultValue) {
    }
}
