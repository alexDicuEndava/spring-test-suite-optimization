# Spring Test-Suite Optimization Demo

This repository demonstrates how Spring Boot test-suite design changes feedback time and CI runner usage. The comparison uses two branches with the same bookkeeping CRUD intent:

- `demo/bad-test-suite`: one broad test task with intentionally inefficient suite design and full integration tests that dirty the Spring context after each test method.
- `demo/optimized-test-suite`: unit, slice, and integration tests separated for context reuse and fail-fast CI.

The sample application is a small bookkeeping REST API for customers and invoices. It includes JPA persistence, validation, minimal security for write endpoints, a cacheable customer lookup, and a test-only 2 second Spring context startup delay that is distributed across core bookkeeping bean initialization so context recreation cost is visible.

## Local Comparison

Run each branch from a clean checkout and compare elapsed time:

```bash
git switch demo/bad-test-suite
time ./gradlew clean test

git switch demo/optimized-test-suite
time ./gradlew clean comparisonTest

# CI/fail-fast shape:
time ./gradlew clean unitTest
time ./gradlew sliceTest
time ./gradlew integrationTest
time ./gradlew check
```

Use `comparisonTest` for the fairest local wall-clock comparison against the bad branch's single broad `test` task. Use `check` to see the CI/fail-fast shape. The optimized branch is expected to spend less time recreating Spring application contexts. Unit tests run without Spring and are parallelized by JUnit. Slice tests use focused Spring contexts. Full integration tests share stable configuration and clear database/cache state explicitly.

## CI Shape

The bad branch runs one broad `test` job. The optimized branch has two workflow shapes:

- `Test suite - fail fast`: runs `unitTest`, then `sliceTest`, then `integrationTest` as separate gated jobs.
- `Test suite - single job`: runs `./gradlew check` in one job to show the staged suite in one CI job. For local one-task timing, use `./gradlew comparisonTest`.

Each workflow writes task duration to the GitHub Actions step summary. Spring test context cache logging is enabled for Spring-based tasks so timing can be connected to context reuse.

## Runner Cost Formula

Use your own runner price:

```text
estimated savings = (bad branch minutes - optimized branch minutes) * cost per runner minute
```

Example with a configurable rate:

```text
bad branch:        12.0 minutes
optimized branch:   5.0 minutes
saved:              7.0 minutes
rate:              $0.008 per minute
estimated savings:  7.0 * 0.008 = $0.056 per run
```

The absolute numbers depend on hardware and cache state. The presentation point is the direction and cause of the difference: fewer Spring context recreations, cheaper tests first, and less runner time spent after early failures.
