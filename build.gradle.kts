import com.github.gradle.node.npm.task.NpmInstallTask
import com.github.gradle.node.npm.task.NpmTask
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.wrapper.Wrapper

plugins {
    base
    id("com.github.node-gradle.node") version "7.1.0"
}

tasks.named<Wrapper>("wrapper") {
    gradleVersion = providers.gradleProperty("gradleVersion").get()
}

tasks.register("runIde") {
    dependsOn(":intellij:runIde")
}

tasks.register("runIdeForUiTests") {
    dependsOn(":intellij:runIdeForUiTests")
}

tasks.register("buildPlugin") {
    dependsOn(":intellij:buildPlugin")
    doLast {
        copy {
            from(layout.projectDirectory.dir("intellij/build/distributions"))
            into(layout.projectDirectory.dir("build/distributions"))
            include("*.zip")
        }
    }

    dependsOn("buildVscodeExtension")
}

tasks.register("verifyPlugin") {
    dependsOn(":intellij:verifyPlugin")
}

tasks.register("publishPlugin") {
    dependsOn(":intellij:publishPlugin")
}

tasks.register("patchPluginXml") {
    dependsOn(":intellij:patchPluginXml")
}

tasks.register("signPlugin") {
    dependsOn(":intellij:signPlugin")
}

val vscodeDir = layout.projectDirectory.dir("vscode")

// Copy CHANGELOG.md from root to vscode for packaging
val copyChangelogToVscode = tasks.register("copyChangelogToVscode") {
    val src = layout.projectDirectory.file("CHANGELOG.md")
    val dst = vscodeDir.file("CHANGELOG.md")

    inputs.file(src)
    outputs.file(dst)

    doLast {
        copy {
            from(src)
            into(vscodeDir)
        }
    }
}

// Configure the Node plugin to run in vscode/ and to download a pinned Node version.
node {
    nodeProjectDir.set(vscodeDir.asFile)
    download.set(true)
    version.set(providers.gradleProperty("nodeVersion"))

    // Default to "install" unless you explicitly set npmInstallCommand=ci in gradle.properties.
    npmInstallCommand.set(providers.gradleProperty("npmInstallCommand").orElse("install"))
}

// Configure the plugin-provided npmInstall task (runs in nodeProjectDir)
val npmInstall = tasks.named<NpmInstallTask>("npmInstall") {
    group = "vscode"
    description = "Installs VS Code extension dependencies (npm install/ci) using plugin-managed Node."

    // The extension depends on the locally-built Kotlin/JS package:
    // "copy4lm-common": "file:../common/build/dist/js/productionLibrary" :contentReference[oaicite:4]{index=4}
    dependsOn(":common:jsNodeProductionLibraryDistribution") // matches the documented build order :contentReference[oaicite:5]{index=5}

    inputs.dir(layout.projectDirectory.dir("common/build/dist/js/productionLibrary"))

    inputs.file(vscodeDir.file("package.json"))
    inputs.file(vscodeDir.file("package-lock.json")).optional()

    outputs.dir(vscodeDir.dir("node_modules"))
}

// Keep your original task name as an alias (optional, but preserves your current UX)
tasks.register("vscodeNpmInstall") {
    group = "vscode"
    description = "Alias for npmInstall (plugin-managed Node)."
    dependsOn(npmInstall)
}

val vscodeBuild = tasks.register<NpmTask>("vscodeBuild") {
    group = "vscode"
    description = "Builds the VS Code extension bundle (npm run build)."

    dependsOn(npmInstall)
    dependsOn(copyChangelogToVscode)

    inputs.file(vscodeDir.file("esbuild.config.mjs"))
    inputs.file(vscodeDir.file("tsconfig.json"))
    inputs.dir(vscodeDir.dir("src"))
    // output is dist/extension.js per esbuild config :contentReference[oaicite:6]{index=6}
    outputs.dir(vscodeDir.dir("dist"))

    npmCommand.set(listOf("run", "build")) // package.json: "build": "node esbuild.config.mjs" :contentReference[oaicite:7]{index=7}
}

val vscodePackage = tasks.register<NpmTask>("vscodePackage") {
    group = "vscode"
    description = "Packages the VS Code extension (.vsix) (npm run package)."

    dependsOn(vscodeBuild)

    // vsce writes a *.vsix into the vscode/ folder by default
    outputs.files(fileTree(vscodeDir) { include("*.vsix") })

    npmCommand.set(listOf("run", "package")) // package.json: "package": "vsce package" :contentReference[oaicite:8]{index=8}
}

val stageVscodeVsix = tasks.register<Copy>("stageVscodeVsix") {
    group = "distribution"
    description = "Stages the VS Code extension .vsix into build/distributions."

    dependsOn(vscodePackage)
    from(fileTree(vscodeDir) { include("*.vsix") })
    into(layout.projectDirectory.dir("build/distributions"))
}

tasks.register("buildVscodeExtension") {
    group = "distribution"
    description = "Builds and stages the VS Code extension .vsix into build/distributions."

    dependsOn(stageVscodeVsix)
}

// Optional: wire into the root lifecycle
tasks.named("assemble") {
    dependsOn("buildVscodeExtension")
}

// Optional: clean VS Code artifacts on ./gradlew clean
tasks.named<Delete>("clean") {
    delete(
        vscodeDir.dir("dist"),
        vscodeDir.dir("node_modules"),
        fileTree(vscodeDir) { include("*.vsix") }
    )
}
