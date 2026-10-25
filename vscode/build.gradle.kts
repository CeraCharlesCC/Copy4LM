import com.github.gradle.node.npm.task.NpmInstallTask
import com.github.gradle.node.npm.task.NpmTask

plugins {
    base
    alias(libs.plugins.nodeGradle)
}

val vscodeDir = layout.projectDirectory

// Configure the Node plugin to run in vscode/ and to download a pinned Node version.
node {
    nodeProjectDir.set(vscodeDir.asFile)
    download.set(true)
    version.set(providers.gradleProperty("nodeVersion"))

    // Default to "install" unless you explicitly set npmInstallCommand=ci in gradle.properties.
    npmInstallCommand.set(providers.gradleProperty("npmInstallCommand").orElse("install"))
}

// Copy CHANGELOG.md from root to vscode for packaging
val copyChangelogToVscode = tasks.register("copyChangelogToVscode") {
    val src = rootProject.layout.projectDirectory.file("CHANGELOG.md")
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

// Configure the plugin-provided npmInstall task (runs in nodeProjectDir)
val npmInstall = tasks.named<NpmInstallTask>("npmInstall") {
    group = "vscode"
    description = "Installs VS Code extension dependencies (npm install/ci) using plugin-managed Node."

    // The extension depends on the locally-built Kotlin/JS package:
    // "copy4lm-common": "file:../common/build/dist/js/productionLibrary"
    dependsOn(":common:jsNodeProductionLibraryDistribution")

    inputs.dir(project(":common").layout.buildDirectory.dir("dist/js/productionLibrary"))

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
    inputs.dir(project(":common").layout.buildDirectory.dir("dist/js/productionLibrary"))
    // output is dist/extension.js per esbuild config
    outputs.dir(vscodeDir.dir("dist"))

    npmCommand.set(listOf("run", "build")) // package.json: "build": "node esbuild.config.mjs"
}

val vscodeTypecheck = tasks.register<NpmTask>("vscodeTypecheck") {
    group = "verification"
    description = "Type-checks the VS Code adapter against the generated Kotlin/JS declarations."
    dependsOn(npmInstall)
    npmCommand.set(listOf("run", "typecheck"))
}

val vscodeTest = tasks.register<NpmTask>("vscodeTest") {
    group = "verification"
    description = "Runs VS Code adapter regression tests."
    dependsOn(vscodeBuild)
    npmCommand.set(listOf("run", "test:unit"))
}

tasks.named("check") {
    dependsOn(vscodeTypecheck, vscodeTest)
}

val vscodePackage = tasks.register<NpmTask>("vscodePackage") {
    group = "vscode"
    description = "Packages the VS Code extension (.vsix) (npm run package)."

    dependsOn(vscodeBuild)

    inputs.dir(vscodeDir.dir("dist"))
    inputs.files(vscodeDir.file("package.json"), vscodeDir.file("README.md"),
        vscodeDir.file("CHANGELOG.md"), vscodeDir.file("LICENSE"), vscodeDir.file(".vscodeignore"))

    // vsce writes a *.vsix into the vscode/ folder by default
    outputs.files(fileTree(vscodeDir) { include("*.vsix") })

    npmCommand.set(listOf("run", "package")) // package.json: "package": "vsce package"
}

// Clean VS Code artifacts on ./gradlew clean
tasks.named<Delete>("clean") {
    delete(
        vscodeDir.dir("dist"),
        vscodeDir.dir("node_modules"),
        fileTree(vscodeDir) { include("*.vsix") }
    )
}
