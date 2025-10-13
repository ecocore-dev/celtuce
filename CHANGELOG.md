# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).


## [0.5.0] - 2025-10-13
### Added
- **Node filtering**:
    - Added support for the `:node-filter` option in `ClusterClientOptions`.
    - Introduced an idiomatic Clojure map representation of cluster nodes for filtering.
    - Applications can now exclude unhealthy nodes more easily,
      improving cluster reliability while keeping the API consistent with other Celtuce options.
- **Command timeout options**: Added support for flexible timeout configurations in `timeout-options`:
    - `:connection-timeout` — apply the connection timeout as the default timeout for commands.
    - `:get-timeout` — custom timeout source function that receives a command type as a kebab-case
      keyword (e.g. `:get`, `:set`) and returns a timeout in milliseconds, enabling per-command timeout strategies.
- Added support for the `:enable-all-adaptive-refresh-triggers` option in
  cluster topology refresh configuration.

### Fixed
- Fixed handling of the `:all` keyword in adaptive refresh triggers to
  avoid attempting sequence operations on keywords.

### Deprecated
- **`carbonite-codec`**: marked as deprecated due to outdated dependencies
  (Carbonite hasn't been updated since 2014, uses Kryo 3 and Java 1.6).  
  Users should migrate to `nippy-codec`, which is actively maintained,
  provides better performance, and is already the default codec in Celtuce.  
  The `carbonite-codec` will be removed in a future major release.


## [0.4.4] - 2025-09-19
### Changed
- chore: add descriptions for `celtuce-pool` and `celtuce-manifold` libraries.

### Added
- Support for `:adaptive-refresh-triggers-timeout` option in cluster client configuration.
- Normalization of `:enable-adaptive-refresh-trigger`: now accepts any collection, automatically deduplicated via set.

### Fixed
- Skip call to `.enableAdaptiveRefreshTrigger()` when the triggers list is empty for Lettuce ≥ 6.4  
  (prevents `IllegalArgumentException` on startup).
- Added type hints for cluster shutdown to eliminate reflection problems (improves runtime performance).


## [0.4.3] - 2025-09-18
### Changed
- Updated dependencies:
    - **Clojure**: 1.10.3 → 1.12.2
    - **Lettuce**: 6.1.1.RELEASE → 6.8.1.RELEASE
    - **nippy**: 3.1.1 → 3.5.0
    - **manifold**: 0.1.8 → 0.4.3
    - **potemkin**: 0.4.5 → 0.4.6
    - **Apache Commons Pool2**: 2.9.0 → 2.12.1

### Added
- Integrated **clj-kondo** into the repository.
- Added clj-kondo configuration exports for the **public API** of `celtuce-pool`.

### Fixed
- Resolved various linting issues (unused vars, missing docstrings, inconsistent requires).

### Refactored
- Minor internal cleanups and small refactorings.
