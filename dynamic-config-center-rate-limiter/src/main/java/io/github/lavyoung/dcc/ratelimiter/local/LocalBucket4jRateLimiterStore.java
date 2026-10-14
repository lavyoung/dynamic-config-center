package io.github.lavyoung.dcc.ratelimiter.local;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.lavyoung.dcc.ratelimiter.core.RateLimiterDecision;
import io.github.lavyoung.dcc.ratelimiter.core.RateLimiterRule;
import io.github.lavyoung.dcc.ratelimiter.core.RateLimiterStore;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 基于 Bucket4j 的本地限流状态存储实现。
 *
 * <p>该实现使用 Bucket4j 提供的令牌桶算法维护单机限流状态。
 * 每一个限流 key 会对应一个本地 {@link Bucket}。请求进入时，
 * 组件尝试从对应 bucket 中消费一个令牌，消费成功则放行，
 * 消费失败则返回限流拒绝结果。</p>
 *
 * <p>该实现只适合单机限流。多实例部署时，不同 JVM 之间不会共享令牌桶、
 * 拒绝次数和临时黑名单。后续 Redis 分布式限流版本应继续实现
 * {@link RateLimiterStore}，使 AOP 层无需感知存储差异。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 0.2.0
 * @date 2026/10/14
 */
public class LocalBucket4jRateLimiterStore implements RateLimiterStore {

    private final LocalRateLimiterOptions options;

    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, RejectCounter> rejectCounters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, BlacklistEntry> blacklist = new ConcurrentHashMap<>();

    /**
     * 使用默认配置创建本地限流状态存储。
     */
    public LocalBucket4jRateLimiterStore() {
        this(LocalRateLimiterOptions.defaults());
    }

    /**
     * 使用指定配置创建本地限流状态存储。
     *
     * @param options 本地限流配置
     * @throws NullPointerException 当配置为空时抛出
     */
    public LocalBucket4jRateLimiterStore(LocalRateLimiterOptions options) {
        this.options = Objects.requireNonNull(options, "options must not be null");
    }

    @Override
    public RateLimiterDecision acquire(String key, RateLimiterRule rule) {
        String requireKey = requireKey(key);
        Objects.requireNonNull(rule, "rule must not be null");

        if (isBlacklisted(requireKey)) {
            return RateLimiterDecision.blacklisted();
        }

        Bucket bucket = getOrCreateBucket(requireKey, rule);
        if (bucket == null) {
            return capacityExceededDecision();
        }

        if (bucket.tryConsume(1)) {
            rejectCounters.remove(requireKey);
            return RateLimiterDecision.allowed();
        }

        recordReject(requireKey, rule);
        return RateLimiterDecision.rateLimited();
    }

    /**
     * 获取或创建指定 key 的本地令牌桶。
     *
     * @param key  限流 key
     * @param rule 限流规则
     * @return 本地令牌桶；容量已满且不允许创建时返回 {@code null}
     */
    private Bucket getOrCreateBucket(String key, RateLimiterRule rule) {
        Bucket bucket = buckets.get(key);
        if (bucket != null) {
            return bucket;
        }

        if (buckets.size() >= options.maxBucketKeys()) {
            return null;
        }

        return buckets.computeIfAbsent(key, ignored -> createBucket(rule));
    }

    /**
     * 根据限流规则创建 Bucket4j 令牌桶。
     *
     * @param rule 限流规则
     * @return 新建的本地令牌桶
     */
    private Bucket createBucket(RateLimiterRule rule) {
        long capacity = Math.max(1L, Math.round(rule.permitsPerSecond()));
        Bandwidth bandwidth = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(capacity, Duration.ofSeconds(1))
                .build();

        return Bucket.builder()
                .addLimit(bandwidth)
                .build();
    }

    /**
     * 判断指定 key 是否命中临时黑名单。
     *
     * @param key 限流 key
     * @return {@code true} 表示命中黑名单；{@code false} 表示没有命中
     */
    private boolean isBlacklisted(String key) {
        BlacklistEntry entry = blacklist.get(key);
        if (entry == null) {
            return false;
        }
        if (entry.isExpired()) {
            blacklist.remove(key);
            return false;
        }
        return true;
    }

    /**
     * 记录一次限流拒绝，并在达到阈值后写入临时黑名单。
     *
     * @param key  限流 key
     * @param rule 限流规则
     */
    private void recordReject(String key, RateLimiterRule rule) {
        if (rule.blacklistCount() == 0) {
            return;
        }

        if (!rejectCounters.containsKey(key) && rejectCounters.size() >= options.maxRejectCounterKeys()) {
            return;
        }

        RejectCounter counter = rejectCounters.compute(key, (ignored, previous) ->
                previous == null ? RejectCounter.first() : previous.next()
        );

        if (counter.count() > rule.blacklistCount()) {
            putBlacklist(key, rule);
            rejectCounters.remove(key);
        }
    }

    /**
     * 写入临时黑名单。
     *
     * @param key  限流 key
     * @param rule 限流规则
     */
    private void putBlacklist(String key, RateLimiterRule rule) {
        if (!blacklist.containsKey(key) && blacklist.size() >= options.maxBlacklistKeys()) {
            return;
        }
        blacklist.put(key, BlacklistEntry.expireAfter(rule.blacklistTtlSeconds()));
    }

    /**
     * 生成容量超过上限时的限流结果。
     *
     * @return 容量超过上限时的判定结果
     */
    private RateLimiterDecision capacityExceededDecision() {
        return options.failOpenWhenCapacityExceeded()
                ? RateLimiterDecision.allowed()
                : RateLimiterDecision.rateLimited();
    }

    /**
     * 校验限流 key。
     *
     * @param key 待校验限流 key
     * @return 去除首尾空白后的限流 key
     * @throws NullPointerException     当限流 key 为空时抛出
     * @throws IllegalArgumentException 当限流 key 只包含空白字符时抛出
     */
    private String requireKey(String key) {
        Objects.requireNonNull(key, "key must not be null");
        String trimmed = key.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("key must not be blank");
        }
        return trimmed;
    }

    /**
     * 本地限流拒绝计数。
     *
     * @param count 当前连续限流拒绝次数
     */
    private record RejectCounter(long count) {

        /**
         * 创建首次拒绝计数。
         *
         * @return 首次拒绝计数
         */
        private static RejectCounter first() {
            return new RejectCounter(1L);
        }

        /**
         * 创建下一次拒绝计数。
         *
         * @return 递增后的拒绝计数
         */
        private RejectCounter next() {
            return new RejectCounter(count + 1L);
        }
    }

    /**
     * 本地临时黑名单条目。
     *
     * @param expiresAtMillis 过期时间戳，单位毫秒
     */
    private record BlacklistEntry(long expiresAtMillis) {

        /**
         * 创建指定秒数后过期的黑名单条目。
         *
         * @param ttlSeconds 过期秒数
         * @return 黑名单条目
         */
        private static BlacklistEntry expireAfter(long ttlSeconds) {
            return new BlacklistEntry(System.currentTimeMillis() + Duration.ofSeconds(ttlSeconds).toMillis());
        }

        /**
         * 判断当前黑名单条目是否已经过期。
         *
         * @return {@code true} 表示已过期；{@code false} 表示仍然有效
         */
        private boolean isExpired() {
            return System.currentTimeMillis() >= expiresAtMillis;
        }
    }
}
