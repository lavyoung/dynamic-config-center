# Changelog

This file records changes that matter to users of the published artifacts.

## [0.2.1-SNAPSHOT]

### Added

- Dynamic rate limiter core module.
- Spring Boot starter for `@DccRateLimiter`.
- Local Bucket4j limiter with reject counters and temporary blacklist support.
- Spring Boot configuration metadata for DCC and rate limiter properties.
- Package-level Javadocs for component packages.
- PR verification workflow.

### Changed

- Expanded the BOM so internal modules can share one version source.
- Reworked README around installation, configuration and extension examples.
- Prepared Maven Central snapshot and release profiles.

## [0.1.0-SNAPSHOT]

### Added

- Core `@DccValue` dynamic configuration binding.
- ZooKeeper-backed configuration source.
- Spring Boot starter for Dynamic Config Center.
