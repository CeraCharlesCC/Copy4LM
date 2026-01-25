import org.gradle.api.tasks.wrapper.Wrapper
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
