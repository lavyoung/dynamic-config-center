# Dynamic Config Center

Dynamic Config Center 是一个面向 Spring Boot 应用的轻量级动态配置组件。

它最初用于把 ZooKeeper 中的配置变更实时绑定到 Java 字段上，后来扩展出基于 DCC 动态开关的限流能力。组件本身不绑定任何业务领域，适合在营销、交易、风控、活动系统等需要动态开关、降级、限流的场景中复用。

## 特性

- 基于 `@DccValue` 的字段级动态配置绑定
- 基于 ZooKeeper `CuratorCache` 的配置监听
- Spring Boot 3 自动装配
- 支持业务侧自定义配置字段和业务含义
- 支持 `@DccRateLimiter` 声明式动态限流
- 默认提供 Bucket4j 本地令牌桶限流实现
- 支持限流 fallback 方法
- 支持限流拒绝计数和临时黑名单
- 支持通过自定义 `RateLimiterStore` 扩展 Redis 等分布式限流实现
- 发布包包含 main jar、sources jar 和 javadoc jar

## 模块说明

```text
dynamic-config-center-parent
├─ dynamic-config-center-core
├─ dynamic-config-center-zookeeper
├─ dynamic-config-center-spring-boot-starter
├─ dynamic-config-center-rate-limiter
├─ dynamic-config-center-rate-limiter-spring-boot-starter
└─ dynamic-config-center-bom
```

| 模块 | 说明 |
| --- | --- |
| `dynamic-config-center-core` | 核心注解、字段绑定器和配置提供者端口 |
| `dynamic-config-center-zookeeper` | 基于 ZooKeeper / CuratorCache 的配置提供者实现 |
| `dynamic-config-center-spring-boot-starter` | DCC 字段动态配置的 Spring Boot 自动装配 |
| `dynamic-config-center-rate-limiter` | 与 Spring 无关的限流核心协议、本地限流实现和工具类 |
| `dynamic-config-center-rate-limiter-spring-boot-starter` | `@DccRateLimiter` 的 Spring Boot 自动装配和 AOP 接入 |
| `dynamic-config-center-bom` | 依赖版本管理 |

## 环境要求

- JDK 17+
- Maven 3.8+
- Spring Boot 3.x
- ZooKeeper 3.7+ 或兼容版本

## 快速开始：动态配置

### 1. 引入依赖

```xml
<dependency>
    <groupId>io.github.lavyoung</groupId>
    <artifactId>dynamic-config-center-spring-boot-starter</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

如果项目统一使用 BOM，可以这样声明：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.github.lavyoung</groupId>
            <artifactId>dynamic-config-center-bom</artifactId>
            <version>0.1.0-SNAPSHOT</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 2. 配置 ZooKeeper

```yaml
dcc:
  enabled: true
  connect-string: 127.0.0.1:2181
  root-path: /market-forge-dcc/config
  session-timeout-ms: 18000
  connection-timeout-ms: 30000
```

### 3. 声明动态字段

```java
package com.example.demo.config;

import io.github.lavyoung.dcc.core.DccValue;
import org.springframework.stereotype.Component;

/**
 * 活动动态配置。
 */
@Component
public class ActivityDccConfig {

    /**
     * 活动降级开关。
     *
     * <p>DCC key 为 {@code activity.degrade.switch}，默认值为 {@code false}。</p>
     */
    @DccValue("activity.degrade.switch:false")
    private volatile boolean degraded;

    /**
     * 判断当前活动是否进入降级状态。
     *
     * @return {@code true} 表示降级；{@code false} 表示正常执行
     */
    public boolean isDegraded() {
        return degraded;
    }
}
```

### 4. ZooKeeper 节点示例

如果 `root-path` 配置为：

```text
/market-forge-dcc/config
```

那么动态开关可以写入：

```text
/market-forge-dcc/config/activity.degrade.switch = true
```

字段值会在节点变更后自动刷新。

## 快速开始：动态限流

### 1. 引入限流 starter

```xml
<dependency>
    <groupId>io.github.lavyoung</groupId>
    <artifactId>dynamic-config-center-rate-limiter-spring-boot-starter</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

如果已经使用 `dynamic-config-center-bom`，可以省略 `version`。

### 2. 开启限流自动装配

```yaml
dcc:
  rate-limiter:
    enabled: true
    fail-open-when-capacity-exceeded: true
    local:
      max-bucket-keys: 100000
      max-reject-counter-keys: 100000
      max-blacklist-keys: 100000
```

这组配置只控制 starter 是否创建限流相关 Bean。

真正运行时是否执行限流，由 DCC 动态 key 控制：

```text
rate-limiter.enabled=true
```

两者职责不同：

| 配置 | 作用 |
| --- | --- |
| `dcc.rate-limiter.enabled` | 应用启动时是否装配限流组件 |
| `rate-limiter.enabled` | 运行时是否真正执行限流判断 |

### 3. 使用 `@DccRateLimiter`

```java
package com.example.demo.service;

import io.github.lavyoung.dcc.ratelimiter.annotation.DccRateLimiter;
import org.springframework.stereotype.Service;

/**
 * 积分兑换服务。
 */
@Service
public class CreditExchangeService {

    /**
     * 使用积分兑换商品。
     *
     * @param request 兑换请求
     * @return 兑换结果
     */
    @DccRateLimiter(
            key = "userId",
            fallbackMethod = "exchangeFallback",
            permitsPerSecond = 1.0,
            blacklistCount = 3,
            blacklistTtlSeconds = 60
    )
    public String exchange(ExchangeRequest request) {
        return "exchange success: " + request.userId();
    }

    /**
     * 兑换限流 fallback。
     *
     * <p>fallback 方法必须与原方法位于同一个类或父类中，并保持相同参数列表。</p>
     *
     * @param request 兑换请求
     * @return 限流后的兜底结果
     */
    private String exchangeFallback(ExchangeRequest request) {
        return "too many requests: " + request.userId();
    }

    /**
     * 兑换请求。
     *
     * @param userId 用户 ID
     * @param productId 商品 ID
     */
    public record ExchangeRequest(String userId, Long productId) {
    }
}
```

### 4. 注解参数说明

| 参数 | 说明 |
| --- | --- |
| `key` | 从方法参数中解析限流 key，例如 `userId` |
| `fallbackMethod` | 被限流或命中黑名单时调用的 fallback 方法名 |
| `permitsPerSecond` | 每秒允许通过的请求数 |
| `blacklistCount` | 连续拒绝达到多少次后加入临时黑名单，`0` 表示不启用 |
| `blacklistTtlSeconds` | 临时黑名单有效期，单位秒 |

## 扩展分布式限流

默认限流实现是本地内存 + Bucket4j，适合单机或课程阶段验证。如果需要多实例共享限流状态，可以实现自己的 `RateLimiterStore`：

```java
package com.example.demo.ratelimiter;

import io.github.lavyoung.dcc.ratelimiter.core.RateLimiterDecision;
import io.github.lavyoung.dcc.ratelimiter.core.RateLimiterRule;
import io.github.lavyoung.dcc.ratelimiter.core.RateLimiterStore;
import org.springframework.stereotype.Component;

/**
 * 基于 Redis 的分布式限流存储示例。
 */
@Component
public class RedisRateLimiterStore implements RateLimiterStore {

    /**
     * 获取一次限流令牌。
     *
     * @param key 限流 key
     * @param rule 限流规则
     * @return 限流判定结果
     */
    @Override
    public RateLimiterDecision acquire(String key, RateLimiterRule rule) {
        // 在这里接入 Redis Lua、Redisson RateLimiter 或 Bucket4j Redis 扩展。
        return RateLimiterDecision.allowed();
    }
}
```

starter 中的默认 Bean 使用了 `@ConditionalOnMissingBean`，因此业务项目提供自己的 `RateLimiterStore` 后，默认本地实现会自动让位。

## 构建与测试

运行全部测试：

```bash
mvn clean test
```

只测试限流 starter：

```bash
mvn -q -pl dynamic-config-center-rate-limiter-spring-boot-starter -am test
```

打包并生成源码包、Javadoc 包：

```bash
mvn clean package
```

安装到本地 Maven 仓库：

```bash
mvn clean install
```

## 设计原则

- 核心模块不依赖 Spring，保持可复用。
- Spring Boot starter 只负责自动装配，不承载业务逻辑。
- 动态配置只负责刷新值，不决定业务语义。
- 限流切面只负责编排调用，不直接维护限流状态。
- 默认实现适合快速接入，复杂场景通过端口接口扩展。

## Roadmap

- Redis 分布式限流实现
- 更细粒度的限流开关 key
- 与 Resilience4j 集成的熔断和超时保护
- Spring Boot Actuator 观测端点
- 配置变更审计和版本记录
