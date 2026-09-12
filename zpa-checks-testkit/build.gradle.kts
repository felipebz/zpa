import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.publish.maven.MavenPublication

plugins {
    id("com.felipebz.zpa.build-conventions")
    alias(libs.plugins.shadow)
}

dependencies {
    compileOnly(project(":zpa-core"))
}

val rawTestkitJar = tasks.register<Jar>("rawTestkitJar") {
    archiveClassifier.set("raw")
    from(sourceSets.main.get().output)
}

configurations.create("rawRuntimeElements") {
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
        attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
    }
    outgoing.artifact(rawTestkitJar)
}

val shadowJar = tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("")
    configurations = emptyList()
    relocate("com.felipebz.flr.api", "org.sonar.plugins.plsqlopen.api.sslr")
    relocate("com.felipebz.zpa.api", "org.sonar.plugins.plsqlopen.api")
}

tasks.jar {
    enabled = false
}

tasks.build {
    dependsOn(shadowJar)
}

publishing {
    publications.withType<MavenPublication> {
        artifact(shadowJar)
    }
}

// The published artifact is the shaded testkit. Gradle module metadata would otherwise expose
// the disabled raw Java component as the default variant and hide the shaded artifact.
tasks.withType<GenerateModuleMetadata> {
    enabled = false
}

description = "ZPA Checks TestKit"
