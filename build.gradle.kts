import org.gradle.api.tasks.wrapper.Wrapper
import org.gradle.api.tasks.Exec
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.register

plugins {
    base
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

// Convenience: all npm commands run with --prefix vscode (same pattern as scripts/build_vscode.sh) :contentReference[oaicite:3]{index=3}
fun Exec.npm(vararg args: String) {
    commandLine(listOf("npm", "--prefix", "vscode") + args)
}

val vscodeNpmInstall = tasks.register<Exec>("vscodeNpmInstall") {
    group = "vscode"
    description = "Installs VS Code extension dependencies (npm install)."

    // The extension depends on the locally-built Kotlin/JS package:
    // "copy4lm-common": "file:../common/build/dist/js/productionLibrary" :contentReference[oaicite:4]{index=4}
    dependsOn(":common:jsNodeProductionLibraryDistribution") // matches the documented build order :contentReference[oaicite:5]{index=5}

    inputs.file(vscodeDir.file("package.json"))

    outputs.dir(vscodeDir.dir("node_modules"))

    npm("install")
}

val vscodeBuild = tasks.register<Exec>("vscodeBuild") {
    group = "vscode"
    description = "Builds the VS Code extension bundle (npm run build)."

    dependsOn(vscodeNpmInstall)

    inputs.file(vscodeDir.file("esbuild.config.mjs"))
    inputs.file(vscodeDir.file("tsconfig.json"))
    inputs.dir(vscodeDir.dir("src"))
    // output is dist/extension.js per esbuild config :contentReference[oaicite:6]{index=6}
    outputs.dir(vscodeDir.dir("dist"))

    npm("run", "build") // package.json: "build": "node esbuild.config.mjs" :contentReference[oaicite:7]{index=7}
}

val vscodePackage = tasks.register<Exec>("vscodePackage") {
    group = "vscode"
    description = "Packages the VS Code extension (.vsix) (npm run package)."

    dependsOn(vscodeBuild)

    // vsce writes a *.vsix into the vscode/ folder by default
    outputs.files(fileTree(vscodeDir) { include("*.vsix") })

    npm("run", "package") // package.json: "package": "vsce package" :contentReference[oaicite:8]{index=8}
}

tasks.register("buildVscodeExtension") {
    group = "distribution"
    description = "Builds and stages the VS Code extension .vsix into build/distributions."

    dependsOn(vscodePackage)

    doLast {
        copy {
            from(fileTree(vscodeDir) { include("*.vsix") })
            into(layout.projectDirectory.dir("build/distributions"))
        }
    }
}

// Optional: wire into the root lifecycle
tasks.named("assemble") {
    dependsOn("buildVscodeExtension")
}

// Optional: clean VS Code artifacts on ./gradlew clean
tasks.named("clean") {
    doLast {
        delete(
            vscodeDir.dir("dist"),
            vscodeDir.dir("node_modules"),
            fileTree(vscodeDir) { include("*.vsix") }
        )
    }
}