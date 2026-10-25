# Copy 4 LM Plugin

## Description

<!-- Plugin description -->
<p>Copy4LM is a plugin that enables quick copying of selected file or directory contents to the clipboard in a customizable format tailored for LLMs (Large Language Models), allowing for rapid contextualization of any part of a codebase.</p>
<h2>This plugin is useful for:</h2>
<ul>
    <li>Developers who frequently need to share their code via chat applications.</li>
    <li>Developers who need to copy their code to build context for LLM apps like GPT-5.2 Pro, Gemini 3 Deepthink</li>
</ul>
<h2>Main features:</h2>
<ul>
    <li>Copy the content of multiple files or directories to the clipboard.</li>
    <li>Copy selected relative or absolute path lists with customizable start, end, and delimiter text.</li>
    <li>Customizable text structure, including pre-text, file header text, and post-text.</li>
    <li>Configurable file copying constraints (default is 30 files) to prevent memory issues with larger folders.</li>
    <li>Optional file extension filters to specify which files should be copied.</li>
    <li>Optional <code>.gitignore</code> support to skip ignored files and directories.</li>
    <li>Information on the copied items, including the number of files copied, total lines, total words, and an estimate of tokens (considering words and punctuation).</li>
</ul>
<h2>Credits</h2>
<p>This plugin is based on mwguerra's copy-file-content (under MIT License) plugin, available at: <a href="https://github.com/mwguerra/copy-file-content/">here</a>. Thank you mwguerra for the inspiration and foundation!</p>
<!-- Plugin description end -->


## Development

```bash
# Shared JVM regression tests, IntelliJ compilation/tests, VS Code tests and TypeScript checks
./gradlew test

# Package both IDE extensions into build/distributions
./gradlew buildPlugin
```

The common module uses Kotlin/JS `generateTypeScriptDefinitions()`. Result models
(`CopyResult`, `CopyStats`, `DirectoryStructureResult`) are exported directly with
`@JsExport`; their JavaScript implementation and `.d.ts` declarations come from the
same Kotlin definitions. There is no separate result DTO or source generator to keep
in sync. See [Kotlin/JS TypeScript generation](https://kotlinlang.org/docs/js-project-setup.html#generation-of-typescript-declaration-files-d-ts).

`JsFacade.kt` retains the boundary adapters for plain JavaScript options/arrays,
file gateways and numeric file sizes. After changing the common module, Gradle's
VS Code build also rebuilds the bundle when the generated Kotlin/JS package changes.
