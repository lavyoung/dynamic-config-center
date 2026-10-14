package io.github.lavyoung.dcc.ratelimiter.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link RateLimiterKeyResolver} 单元测试。
 *
 * <p>本测试类验证限流 key 解析器对不同业务入参形态的兼容能力。
 * AOP 层后续只会拿到方法入参数组，因此解析器必须稳定支持字符串入参、
 * record 访问器、JavaBean getter、普通字段以及父类字段。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/14
 */
class RateLimiterKeyResolverTest {

    private final RateLimiterKeyResolver resolver = new RateLimiterKeyResolver();

    /**
     * 验证参数本身是字符串时可以直接作为限流 key。
     *
     * <p>目的：支持服务方法直接以 {@code userId} 作为入参的轻量场景。</p>
     */
    @Test
    void shouldResolveKeyFromStringArgument() {
        // Given
        Object[] args = {" user-001 "};

        // When
        String key = resolver.resolve("userId", args);

        // Then
        assertEquals("user-001", key);
    }

    /**
     * 验证可以从 record 访问器中解析限流 key。
     *
     * <p>目的：支持现代 Java record 请求对象，例如 {@code request.userId()}。</p>
     */
    @Test
    void shouldResolveKeyFromRecordAccessor() {
        // Given
        Object[] args = {new RecordRequest(" user-002 ")};

        // When
        String key = resolver.resolve("userId", args);

        // Then
        assertEquals("user-002", key);
    }

    /**
     * 验证可以从 JavaBean getter 中解析限流 key。
     *
     * <p>目的：兼容传统 DTO 中的 {@code getUserId()} 方法。</p>
     */
    @Test
    void shouldResolveKeyFromGetter() {
        // Given
        Object[] args = {new GetterRequest(" user-003 ")};

        // When
        String key = resolver.resolve("userId", args);

        // Then
        assertEquals("user-003", key);
    }

    /**
     * 验证可以从普通字段中解析限流 key。
     *
     * <p>目的：兼容没有 getter 的简单请求对象。</p>
     */
    @Test
    void shouldResolveKeyFromField() {
        // Given
        Object[] args = {new FieldRequest(" user-004 ")};

        // When
        String key = resolver.resolve("userId", args);

        // Then
        assertEquals("user-004", key);
    }

    /**
     * 验证可以从父类字段中解析限流 key。
     *
     * <p>目的：支持请求对象通过父类承载公共用户字段的场景。</p>
     */
    @Test
    void shouldResolveKeyFromParentField() {
        // Given
        Object[] args = {new ChildRequest(" user-005 ")};

        // When
        String key = resolver.resolve("userId", args);

        // Then
        assertEquals("user-005", key);
    }

    /**
     * 验证当前参数解析结果为空白时，会继续尝试后续参数。
     *
     * <p>目的：避免第一个参数中存在空白字段时提前失败，提升多参数方法兼容性。</p>
     */
    @Test
    void shouldSkipBlankResolvedValueAndResolveFromNextArgument() {
        // Given
        Object[] args = {new FieldRequest("   "), new GetterRequest(" user-006 ")};

        // When
        String key = resolver.resolve("userId", args);

        // Then
        assertEquals("user-006", key);
    }

    /**
     * 验证字段名为空白时抛出参数异常。
     *
     * <p>目的：避免注解配置错误时静默放行或解析出错误 key。</p>
     */
    @Test
    void shouldThrowExceptionWhenKeyNameIsBlank() {
        // Given
        Object[] args = {new GetterRequest("user-001")};

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve("   ", args));
    }

    /**
     * 验证无法解析出有效 key 时抛出参数异常。
     *
     * <p>目的：让错误的注解 key 配置在调用阶段明确暴露。</p>
     */
    @Test
    void shouldThrowExceptionWhenKeyCannotBeResolved() {
        // Given
        Object[] args = {new Object()};

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve("userId", args));
    }

    /**
     * 测试用 record 请求对象。
     *
     * @param userId 用户标识
     */
    private record RecordRequest(String userId) {
    }

    /**
     * 测试用 JavaBean 请求对象。
     */
    private static final class GetterRequest {
        private final String userId;

        private GetterRequest(String userId) {
            this.userId = userId;
        }

        private String getUserId() {
            return userId;
        }
    }

    /**
     * 测试用字段请求对象。
     */
    private static final class FieldRequest {
        private final String userId;

        private FieldRequest(String userId) {
            this.userId = userId;
        }
    }

    /**
     * 测试用父类请求对象。
     */
    private static class ParentRequest {
        private final String userId;

        private ParentRequest(String userId) {
            this.userId = userId;
        }
    }

    /**
     * 测试用子类请求对象。
     */
    private static final class ChildRequest extends ParentRequest {
        private ChildRequest(String userId) {
            super(userId);
        }
    }
}
