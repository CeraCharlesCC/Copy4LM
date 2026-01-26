## [0.2.3] - 2026-01-26

### Added
- Add initial VS Code extension support for copying file content and directory structures

### Changed
- Refactor project to Kotlin Multiplatform to enable shared code across platforms
- Move IntelliJ plugin source code into a dedicated `intellij` submodule

### Internal
- Unify build process to use a single Gradle task for packaging both plugins
- Update CI workflows to build and package the VS Code extension
- Refactor imports to use the `copy4lm-common` namespace
- Clean up old media resources and update project documentation

## [0.2.2] - 2026-01-14
### Changed
- Update plugin icons with 'LLM' branding and visual elements.

### Documentation
- Update README with plugin name, description, and credits.

## [0.2.1] - 2026-01-13
### ⚠️ Breaking changes
- Drop support for IntelliJ IDEA 2022.3; minimum supported version is now 2023.3.

### Internal
- Bump version to 0.2.1.

## [0.2.0] - 2026-01-13

### ⚠️ Breaking changes
- Update placeholder syntax to use double dollar signs (e.g., `$$"$PROJECT_NAME"`) to avoid string interpolation issues; existing templates must be updated.

### Added
- Add directory structure copy feature to copy a tree view of selected directories/files (#2).
- Add `$DIRECTORY_STRUCTURE` placeholder for formatting.
- Split settings UI into separate panels for File Content and Directory Structure.

### Changed
- Replace AWT clipboard with IntelliJ platform clipboard integration.
- Update plugin description to clarify LLM context formatting.
- Improve internal domain model structure and encapsulation.

### Internal
- Add GitHub Actions CI workflow.
- Add unit tests for `CopyFilesInteractor` and `FileCollector`.
- Refactor copy logic into `CopyFilesInteractor` and `FileCollector`.

## [0.1.1] - 2026-01-12

### Fixed
- Fix compatibility with older IntelliJ versions by updating service instance retrieval.

### Internal
- Automate `plugin.xml` change-notes update from `CHANGELOG.md` during release.

## [0.1.0] - 2026-01-12

### Added
- Add initial plugin functionality to copy file contents from open tabs or selected files
- Add configurable file size limit setting (default 500 KB) to handle large files
- Add support for custom header and footer formatting with placeholder substitution

### Changed
- Update default header and footer formats to use triple backticks for code blocks
- Rename plugin to "Copy 4 LM" and migrate package to `io.github.ceracharlescc.copy4lm`

### Internal
- Refactor architecture to hex-c style with ports, adapters, and domain layers
- Add unit tests for use cases, domain logic, and formatting utilities
- Configure GitHub Actions workflow for automated releases and changelog generation
- Update test dependencies to JUnit Jupiter 5.14.1

<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Copy4LM Changelog

## [Unreleased]
