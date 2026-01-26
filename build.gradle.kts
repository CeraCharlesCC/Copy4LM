import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.wrapper.Wrapper

plugins {
    base
}

// ─────────────────────────────────────────────────────────────────────────────
// Gradle Wrapper configuration
// ─────────────────────────────────────────────────────────────────────────────

tasks.named<Wrapper>("wrapper") {
    gradleVersion = providers.gradleProperty("gradleVersion").get()
}

// ─────────────────────────────────────────────────────────────────────────────
// IntelliJ plugin forwarder tasks
// ─────────────────────────────────────────────────────────────────────────────

tasks.register("runIde") {
    dependsOn(":intellij:runIde")
}

tasks.register("runIdeForUiTests") {
    dependsOn(":intellij:runIdeForUiTests")
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

// ─────────────────────────────────────────────────────────────────────────────
// Distribution staging tasks
// ─────────────────────────────────────────────────────────────────────────────

val stageIntellijZip = tasks.register<Copy>("stageIntellijZip") {
    group = "distribution"
    description = "Stages the IntelliJ plugin .zip into build/distributions."

    dependsOn(":intellij:buildPlugin")
    from(layout.projectDirectory.dir("intellij/build/distributions")) {
        include("*.zip")
    }
    into(layout.buildDirectory.dir("distributions"))
}

val stageVscodeVsix = tasks.register<Copy>("stageVscodeVsix") {
    group = "distribution"
    description = "Stages the VS Code extension .vsix into build/distributions."

    dependsOn(":vscode:vscodePackage")
    from(layout.projectDirectory.dir("vscode")) {
        include("*.vsix")
    }
    into(layout.buildDirectory.dir("distributions"))
}

// ─────────────────────────────────────────────────────────────────────────────
// Main buildPlugin entry point (CI/Release compatible)
// ─────────────────────────────────────────────────────────────────────────────

tasks.register("buildPlugin") {
    group = "distribution"
    description = "Builds all plugins and stages artifacts into build/distributions."

    dependsOn(stageIntellijZip)
    dependsOn(stageVscodeVsix)
}

// Wire into the root lifecycle
tasks.named("assemble") {
    dependsOn("buildPlugin")
}
