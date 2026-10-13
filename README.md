# Dynamic Config Center

Reusable Spring Boot dynamic configuration component backed by ZooKeeper.

## Modules

- `dynamic-config-center-core`: annotation, field binding and provider port.
- `dynamic-config-center-zookeeper`: CuratorCache based provider implementation.
- `dynamic-config-center-spring-boot-starter`: Spring Boot auto-configuration.
- `dynamic-config-center-bom`: dependency version management.

## Usage

```xml
<dependency>
    <groupId>io.github.lavyoung</groupId>
    <artifactId>dynamic-config-center-spring-boot-starter</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

```yaml
dcc:
  enabled: true
  connect-string: 192.168.3.127:2181
  root-path: /market-forge-dcc/config
  session-timeout-ms: 18000
  connection-timeout-ms: 30000
```

```java
@Component
public class ActivityDccConfig {

    @DccValue("activity.degrade.switch:false")
    private volatile boolean degraded;

    public boolean isDegraded() {
        return degraded;
    }
}
```

The component contains no business-domain dependency. A business application owns
its configuration fields and decides what a changed value means.
