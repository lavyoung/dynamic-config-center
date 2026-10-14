/**
 * Dynamic Config Center 核心能力包。
 *
 * <p>本包只定义动态配置中心的最小稳定内核：配置数据源端口、
 * 配置字段注解以及字段绑定器。它不依赖 Spring、ZooKeeper、Redis、
 * Web 容器或任何具体业务工程，因此可以被不同运行环境复用。</p>
 *
 * <p>职责边界：</p>
 *
 * <ul>
 *     <li>定义 {@link io.github.lavyoung.dcc.core.DccValue}，描述字段与配置键的绑定关系；</li>
 *     <li>定义 {@link io.github.lavyoung.dcc.core.DccConfigSource}，隔离 ZooKeeper、Redis 等配置来源；</li>
 *     <li>提供 {@link io.github.lavyoung.dcc.core.DccFieldBinder}，完成字段扫描、默认值写入、监听注册和基础类型转换；</li>
 *     <li>不处理 Spring Bean 生命周期、AOP 代理、限流、熔断、业务黑白名单等上层能力。</li>
 * </ul>
 *
 * <p>扩展约定：新的配置来源应实现 {@link io.github.lavyoung.dcc.core.DccConfigSource}；
 * 新的业务保护能力，例如动态限流、动态熔断，应作为独立模块依赖本包，
 * 不应反向污染 core 的配置同步职责。</p>
 *
 * @author lavyoung
 * @since 0.1.0
 */
package io.github.lavyoung.dcc.core;
