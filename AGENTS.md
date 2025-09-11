# Agent Guidelines for Celtuce

## Build & Test Commands
- `lein test` - Run all tests
- `lein test celtuce.connector-test` - Run single test namespace  
- `lein test :only celtuce.connector-test/redis-connector-options-test` - Run specific test
- `lein check` - Check for compilation issues
- `lein do clean, test` - Clean build and run tests

## Code Style Guidelines
- Use kebab-case for function/var names, PascalCase for protocols/records
- Namespace organization: group requires alphabetically, separate core vs external libs
- Use `(:refer-clojure :exclude [...])` when overriding core functions (e.g., get, set)
- Prefer `potemkin/import-vars` for re-exposing functions from submodules
- Enable reflection warnings with `{:global-vars {*warn-on-reflection* true}}`
- Use type hints on Java interop to avoid reflection
- Dynamic vars use `^:dynamic *var*` naming convention with earmuffs
- Protocol methods: docstrings directly under method name
- Testing: use `defmacro` for common test setup patterns like `with-str-cmds`
- Import Java classes explicitly in `:import` section, group by package
- Constants use kw->enum maps (e.g., `kw->tunit`, `kw->cunit`)
- File organization: protocols first, then implementations, then helper functions

## Module Structure
- celtuce-core: Core Redis functionality (required)  
- celtuce-pool: Connection pooling
- celtuce-manifold: Async operations with Manifold