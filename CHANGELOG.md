# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).


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
