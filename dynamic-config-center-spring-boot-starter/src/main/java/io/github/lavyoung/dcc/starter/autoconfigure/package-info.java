/**
 * Dynamic Config Center Spring Boot 自动装配包。
 *
 * <p>本包面向 Spring Boot 应用，负责把 core 与具体配置数据源装配为
 * Spring 容器可直接使用的能力。业务项目引入 starter 后，只需要提供
 * {@code dcc.*} 配置并在字段上声明 {@link io.github.lavyoung.dcc.core.DccValue}，
 * 即可获得动态配置绑定能力。</p>
 *
 * <p>职责边界：</p>
 *
 * <ul>
 *     <li>声明 {@link DccProperties}，承载 starter 配置；</li>
 *     <li>创建 Curator 客户端和 ZooKeeper 配置数据源；</li>
 *     <li>创建 {@link io.github.lavyoung.dcc.core.DccFieldBinder}；</li>
 *     <li>通过 Spring {@code BeanPostProcessor} 自动绑定业务 Bean 中的 DCC 字段；</li>
 *     <li>不定义业务配置项，不实现限流、熔断、黑白名单等具体业务保护逻辑。</li>
 * </ul>
 *
 * <p>与扩展模块的关系：动态限流、动态熔断等能力应作为独立 starter
 * 依赖本 starter 或 core 暴露的配置能力。它们可以复用 DCC 动态开关，
 * 但不应把 AOP 拦截、fallback 调用、Redis 限流状态等职责放入本包。</p>
 *
 * @author lavyoung
 * @since 0.1.0
 */
package io.github.lavyoung.dcc.starter.autoconfigure;
