# Copy 4 LM (VS Code)

Copy file contents or directory structure into the clipboard with LLM-friendly formatting.

## Commands

- **Copy for LLM (contents)**: Copy selected files/folders from Explorer.
- **Copy for LLM (open editors)**: Copy all open editors.
- **Copy for LLM (tree)**: Copy directory structure for the selection.
- **Copy current file for LLM**: Copy the active file.

## Placeholders

- `$PROJECT_NAME`
- `$FILE_PATH`
- `$DIRECTORY_STRUCTURE`

## Settings

- `copy4lm.common.fileCountLimit`
- `copy4lm.common.setMaxFileCount`
- `copy4lm.common.maxFileSizeKB`
- `copy4lm.common.useFilenameFilters`
- `copy4lm.common.filenameFilters`
- `copy4lm.common.strictMemoryRead`
- `copy4lm.fileContent.headerFormat`
- `copy4lm.fileContent.footerFormat`
- `copy4lm.fileContent.preText`
- `copy4lm.fileContent.postText`
- `copy4lm.fileContent.addExtraLineBetweenFiles`
- `copy4lm.directoryStructure.preText`
- `copy4lm.directoryStructure.postText`

## Development

Build the shared Kotlin/JS package first:

```
./gradlew :common:jsProductionLibraryDistribution
```

Then build the extension:

```
cd vscode
npm install
npm run build
npm run package
```
