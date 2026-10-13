package io.github.lavyoung.dcc.core;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link DccFieldBinder} 单元测试。
 *
 * @author lavyoung
 * @since 0.1.0
 */
class DccFieldBinderTest {

    /**
     * 验证默认值绑定、动态更新和关闭资源。
     *
     * @throws Exception 模拟数据源注册失败时抛出
     */
    @Test
    void shouldBindUpdateAndCloseField() throws Exception {
        FakeSource source = new FakeSource();
        DccFieldBinder binder = new DccFieldBinder(source);
        Target target = new Target();

        binder.bind(target);
        assertEquals(false, target.enabled);

        source.publish("true");
        assertEquals(true, target.enabled);

        binder.close();
        assertEquals(1, source.closedCount);
    }

    /**
     * 验证格式错误的配置声明被拒绝。
     */
    @Test
    void shouldRejectMalformedDefinition() {
        DccFieldBinder binder = new DccFieldBinder(new FakeSource());

        assertThrows(
                IllegalArgumentException.class,
                () -> binder.bind(new InvalidTarget())
        );
    }

    private static final class Target {
        @DccValue("feature.enabled:false")
        private boolean enabled;
    }

    private static final class InvalidTarget {
        @DccValue("feature.enabled")
        private boolean enabled;
    }

    private static final class FakeSource implements DccConfigSource {
        private final List<Consumer<String>> consumers = new ArrayList<>();
        private int closedCount;

        @Override
        public AutoCloseable watch(String key, Consumer<String> valueConsumer) {
            consumers.add(valueConsumer);
            return () -> closedCount++;
        }

        private void publish(String value) {
            consumers.forEach(consumer -> consumer.accept(value));
        }

        @Override
        public void close() {
            consumers.clear();
        }
    }
}
