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
