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


## Install

Download the ZIP or VSIX from the [latest GitHub Release](https://github.com/CeraCharlesCC/Copy4LM/releases/latest).

- **IntelliJ IDEA:** Settings → Plugins → gear menu → Install Plugin from Disk → select `copy4lm-intellij-X.Y.Z.zip`, then restart when prompted.
- **VS Code:** Extensions → … → Install from VSIX → select `copy4lm-vscode-X.Y.Z.vsix`. Alternatively, run `code --install-extension /path/to/copy4lm-vscode-X.Y.Z.vsix`.

Download `SHA256SUMS` alongside the packages and run `sha256sum --check SHA256SUMS` to verify them.

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

## Releases and CI

CI runs on pull requests, pushes to `main`, and manual dispatch. It tests the shared
JVM core and VS Code adapter, type-checks TypeScript, compiles the IntelliJ plugin,
and uploads both installable packages in the `plugins` artifact (kept for 14 days).
CI and Release use the same reusable build workflow and Gradle cache; avoid `clean`
unless you need to discard local build outputs. Cache cleanup removes unused Gradle
dependencies after successful builds. PRs restore the cache without writing to it.

To release:

1. Add an entry to `CHANGELOG.md` for the new stable version.
2. Run `bash scripts/update_version.sh X.Y.Z`. This updates Gradle, plugin.xml,
   package.json, package-lock.json, both changelogs, and IntelliJ change notes.
3. Review and commit with `chore: version changed to X.Y.Z`, then push.
4. Run **Release** in GitHub Actions on that branch with `releaseTag=vX.Y.Z`, or use
   `gh workflow run release.yml --ref main -f releaseTag=vX.Y.Z`.

Release validates the committed version before building. It checks the manifests
inside both packages, creates SHA-256 checksums and provenance attestations, then
tags the tested commit and publishes the GitHub Release after uploading all assets.
A failed publication can be retried at the same commit while the release is a draft.
Published releases and tags pointing to another commit are never overwritten.

`generateNotes` defaults to true and uses the existing `LLM_API_KEY`, `LLM_BASE_URL`,
and `LLM_MODEL` Actions secrets to generate GitHub release notes. Set the base URL
to `https://your-provider/v1`; a trailing `/chat/completions` is also normalized.
If generation fails, the committed changelog is used and the workflow reports a
warning. Set `generateNotes=false` to skip the API call. Packaged change notes always
come from the reviewed changelog in the tested commit. No marketplace credentials
are needed to install the GitHub Release packages.
