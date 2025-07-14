plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.license.gradle.plugin)
    implementation(libs.dokka.javadoc.plugin)
    implementation(libs.jreleaser.gradle.plugin)
    implementation(libs.sonarqube.gradle.plugin)
}
