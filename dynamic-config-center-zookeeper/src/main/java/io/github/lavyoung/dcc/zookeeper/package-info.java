/**
 * ZooKeeper 配置数据源适配包。
 *
 * <p>本包负责把 ZooKeeper 节点变化适配为 Dynamic Config Center
 * 核心层可消费的配置事件。它是 {@code dcc-core} 的基础设施实现，
 * 只依赖核心端口，不承载业务配置语义。</p>
 *
 * <p>职责边界：</p>
 *
 * <ul>
 *     <li>使用 Curator 连接 ZooKeeper；</li>
 *     <li>监听指定配置节点的创建和变更；</li>
 *     <li>将节点字节内容按 UTF-8 转换为字符串配置值；</li>
 *     <li>实现 {@link io.github.lavyoung.dcc.core.DccConfigSource}，供 core 层字段绑定器使用；</li>
 *     <li>不扫描业务 Bean，不处理 Spring 自动装配，不实现限流或熔断策略。</li>
 * </ul>
 *
 * <p>本包可以被 Spring Boot Starter 使用，也可以被非 Spring 应用直接构造使用。
 * 其他配置中心实现，例如 Redis、Nacos、Apollo，应创建独立适配模块，
 * 避免把多种中间件实现混入同一个包。</p>
 *
 * @author lavyoung
 * @since 0.1.0
 */
package io.github.lavyoung.dcc.zookeeper;
