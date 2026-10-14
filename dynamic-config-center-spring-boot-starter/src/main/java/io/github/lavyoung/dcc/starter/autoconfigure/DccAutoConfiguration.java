package io.github.lavyoung.dcc.starter.autoconfigure;

import io.github.lavyoung.dcc.core.DccFieldBinder;
import io.github.lavyoung.dcc.zookeeper.ZooKeeperDccConfigSource;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * DCC Starter 自动配置。
 *
 * <p>业务项目只需引入 Starter 并提供 dcc 配置，即可获得 Curator 客户端、
 * 动态字段绑定器和 Spring Bean 自动扫描能力。
 *
 * @author lavyoung
 * @since 0.1.0
 */
@AutoConfiguration
@ConditionalOnClass(CuratorFramework.class)
@ConditionalOnProperty(prefix = "dcc", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(DccProperties.class)
public class DccAutoConfiguration {

    /**
     * 创建 Curator 客户端。
     *
     * @param properties DCC 配置
     * @return Spring 管理的 Curator 客户端
     */
    @Bean(initMethod = "start", destroyMethod = "close")
    @ConditionalOnMissingBean
    public CuratorFramework dccCuratorClient(DccProperties properties) {
        return CuratorFrameworkFactory.builder()
                .connectString(properties.connectString())
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .sessionTimeoutMs(properties.sessionTimeoutMs())
                .connectionTimeoutMs(properties.connectionTimeoutMs())
                .build();
    }

    /**
     * 创建 ZooKeeper 配置数据源。
     *
     * @param client Curator 客户端
     * @param properties DCC 配置
     * @return ZooKeeper 配置数据源
     */
    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public ZooKeeperDccConfigSource dccConfigSource(
            CuratorFramework client,
            DccProperties properties) {
        return new ZooKeeperDccConfigSource(client, properties.rootPath());
    }

    /**
     * 创建动态配置字段绑定器。
     *
     * @param source 配置数据源
     * @return 字段绑定器
     */
    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public DccFieldBinder dccFieldBinder(io.github.lavyoung.dcc.core.DccConfigSource source) {
        return new DccFieldBinder(source);
    }

    /**
     * 创建 Spring Bean 后置处理器，自动绑定业务 Bean 中的 DCC 字段。
     *
     * @param binder 字段绑定器
     * @return Bean 后置处理器
     */
    @Bean(destroyMethod = "close")
    public BeanPostProcessor dccBeanPostProcessor(DccFieldBinder binder) {
        return new DccBeanPostProcessor(binder);
    }

    private static final class DccBeanPostProcessor implements BeanPostProcessor, AutoCloseable {
        private final DccFieldBinder binder;

        private DccBeanPostProcessor(DccFieldBinder binder) {
            this.binder = binder;
        }

        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) {
            try {
                binder.bind(bean);
                return bean;
            } catch (Exception exception) {
                throw new BeanCreationException(beanName, "DCC binding failed", exception);
            }
        }

        @Override
        public void close() {
            binder.close();
        }
    }
}
