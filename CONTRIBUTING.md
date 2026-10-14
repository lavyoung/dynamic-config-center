# Contributing

This project is still small, so contributions work best when they are focused and easy to review.

## Requirements

- JDK 17+
- Maven 3.8+
- Git

## Build

Run the same command used by the PR workflow:

```bash
mvn -B verify
```

For a quicker local package build:

```bash
mvn clean package -DskipTests
```

## Pull Requests

- Keep a PR scoped to one change.
- Add or update tests when behavior changes.
- Update README and Spring Boot configuration metadata when public configuration changes.
- Do not commit generated build output such as `target/` or `.flattened-pom.xml`.

## Code Style

- Use Java 17 features when they make the code clearer.
- Keep core modules independent from Spring. Spring code belongs in starter modules.
- Keep extension points explicit, especially configuration sources and rate limiter stores.
- Public component APIs need clear Javadocs.

## Release Notes

Update `CHANGELOG.md` for changes that affect users of the published artifacts.
