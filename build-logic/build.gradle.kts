plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.spotless.gradle.plugin)
    implementation(libs.dokka.javadoc.plugin)
    implementation(libs.jreleaser.gradle.plugin)
    implementation(libs.sonarqube.gradle.plugin)
    constraints {
        // Spotless and JReleaser share this class path. JReleaser needs JGit 5 (JGit 7 removed classes it uses), so
        // the version is pinned to its own; Spotless does not load JGit with `LineEnding.PRESERVE` and no ratchet.
        implementation("org.eclipse.jgit:org.eclipse.jgit") {
            version { strictly(libs.versions.jgit.get()) }
        }
    }
}
