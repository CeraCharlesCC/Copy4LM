# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an IntelliJ IDEA plugin called "Copy File Content" that allows users to copy the contents of selected files and directories to the clipboard with customizable formatting. The plugin is built using Kotlin and the IntelliJ Platform Plugin SDK.

## Build & Development Commands

### Running the Plugin in Development
```bash
./gradlew runIde
```
This launches a sandboxed IDE instance with the plugin installed for testing.

### Building the Plugin
```bash
./gradlew build
```
The distributable ZIP file will be created at: `build/distributions/Copy_File_Content-{version}.zip`

### Running Tests
```bash
./gradlew test
```

### Code Coverage
```bash
./gradlew koverHtmlReport
```

### Running UI Tests
```bash
./gradlew runIdeForUiTests
```

### Test build
```bash
./gradlew buildPlugin
```

### Plugin Configuration

The plugin is configured in `intellij/src/main/resources/META-INF/plugin.xml`:
- Registers two actions: one for the file tree context menu, one for the editor tab context menu
- Defines notification group for displaying messages
- Plugin metadata (version, description, change notes) are maintained here
- Version must be kept in sync with `gradle.properties`

### Key Behaviors

- **File Path Resolution**: Uses project content root as the base for relative paths
- **Deduplication**: Tracks copied files by path to avoid duplicates when processing directories
- **Token Estimation**: Simple heuristic based on word count + punctuation count
- **Binary Detection**: Uses IntelliJ's `FileTypeManager` to skip binary files
- **Context Menu Integration**: Actions appear in `CutCopyPasteGroup` and `EditorTabPopupMenu`

## Version Management

The plugin version is maintained in two locations and must be kept in sync:
- `intellij/src/main/resources/META-INF/plugin.xml` (`<version>` tag)
- `gradle.properties` (`pluginVersion` property)

The build process uses `gradle.properties` as the source of truth and patches `plugin.xml` during the build.

## Platform Compatibility

- **Min Build**: 223 (IntelliJ IDEA 2022.3.3+)
- **Target Platform**: IntelliJ Community (`IC`)
- **JVM Toolchain**: Java 17
- **Kotlin**: Managed via Gradle version catalog

## Important Conventions

- When committing version changes, use the format: `chore: version changed to X.Y.Z`
- Always update change notes in `plugin.xml` before releasing
- The plugin description in `plugin.xml` is extracted from README.md during build
- Change notes from CHANGELOG.md or `plugin.xml` are used in the release
