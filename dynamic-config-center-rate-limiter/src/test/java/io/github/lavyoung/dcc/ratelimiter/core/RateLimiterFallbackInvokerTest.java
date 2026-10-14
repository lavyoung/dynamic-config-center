package io.github.lavyoung.dcc.ratelimiter.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link RateLimiterFallbackInvoker} 单元测试。
 *
 * <p>本测试类验证限流 fallback 调用器能按照原方法参数类型定位同类或父类中的
 * 兜底方法，并在方法不存在、方法名非法或 fallback 执行失败时给出明确异常。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/14
 */
class RateLimiterFallbackInvokerTest {

    private final RateLimiterFallbackInvoker invoker = new RateLimiterFallbackInvoker();

    /**
     * 验证可以调用目标类中的 fallback 方法。
     *
     * <p>目的：支撑最常见的业务写法，即原方法和 fallback 方法定义在同一个类中。</p>
     */
    @Test
    void shouldInvokeFallbackMethodOnTargetClass() {
        // Given
        DemoController target = new DemoController();

        // When
        Object result = invoker.invoke(
                target,
                "rateLimited",
                new Class<?>[]{DemoRequest.class},
                new Object[]{new DemoRequest("user-001")}
        );

        // Then
        assertEquals("limited:user-001", result);
    }

    /**
     * 验证可以调用父类中的 fallback 方法。
     *
     * <p>目的：支持业务项目把公共 fallback 方法放在抽象父类中的复用场景。</p>
     */
    @Test
    void shouldInvokeFallbackMethodOnParentClass() {
        // Given
        ChildController target = new ChildController();

        // When
        Object result = invoker.invoke(
                target,
                "parentFallback",
                new Class<?>[]{DemoRequest.class},
                new Object[]{new DemoRequest("user-002")}
        );

        // Then
        assertEquals("parent:user-002", result);
    }

    /**
     * 验证 fallback 方法名为空白时抛出参数异常。
     *
     * <p>目的：避免注解配置错误时在运行期静默失败。</p>
     */
    @Test
    void shouldThrowExceptionWhenFallbackMethodIsBlank() {
        // Given
        DemoController target = new DemoController();

        // When & Then
        assertThrows(
                IllegalArgumentException.class,
                () -> invoker.invoke(target, "   ", new Class<?>[]{DemoRequest.class}, new Object[]{new DemoRequest("user-001")})
        );
    }

    /**
     * 验证 fallback 方法不存在时抛出参数异常。
     *
     * <p>目的：让错误的 fallback 方法名或参数签名尽早暴露。</p>
     */
    @Test
    void shouldThrowExceptionWhenFallbackMethodNotFound() {
        // Given
        DemoController target = new DemoController();

        // When & Then
        assertThrows(
                IllegalArgumentException.class,
                () -> invoker.invoke(target, "missingFallback", new Class<?>[]{DemoRequest.class}, new Object[]{new DemoRequest("user-001")})
        );
    }

    /**
     * 验证 fallback 方法内部抛出异常时会被包装成状态异常。
     *
     * <p>目的：避免反射异常泄露到 AOP 层，同时保留原始异常作为 cause。</p>
     */
    @Test
    void shouldThrowStateExceptionWhenFallbackMethodFails() {
        // Given
        DemoController target = new DemoController();

        // When & Then
        assertThrows(
                IllegalStateException.class,
                () -> invoker.invoke(target, "failedFallback", new Class<?>[]{DemoRequest.class}, new Object[]{new DemoRequest("user-001")})
        );
    }

    /**
     * 测试用请求对象。
     *
     * @param userId 用户标识
     */
    private record DemoRequest(String userId) {
    }

    /**
     * 测试用目标类。
     */
    private static class DemoController {

        /**
         * 测试用限流 fallback 方法。
         *
         * @param request 请求对象
         * @return fallback 结果
         */
        private String rateLimited(DemoRequest request) {
            return "limited:" + request.userId();
        }

        /**
         * 测试用异常 fallback 方法。
         *
         * @param request 请求对象
         * @return 不会返回
         */
        private String failedFallback(DemoRequest request) {
            throw new IllegalStateException("fallback failed: " + request.userId());
        }
    }

    /**
     * 测试用父类。
     */
    private static class ParentController {

        /**
         * 测试用父类 fallback 方法。
         *
         * @param request 请求对象
         * @return fallback 结果
         */
        private String parentFallback(DemoRequest request) {
            return "parent:" + request.userId();
        }
    }

    /**
     * 测试用子类。
     */
    private static final class ChildController extends ParentController {
    }
}
