import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.github.johnrengelman.shadow") version Versions.plugin_shadow
}

dependencies {
    implementation(Libs.flr_toolkit)
    implementation(project(":zpa-core"))
}

val shadowJar = tasks.named<ShadowJar>("shadowJar") {
    minimize()
    archiveClassifier.set("")
    manifest {
        attributes(mapOf("Main-Class" to "org.sonar.plsqlopen.toolkit.PlSqlToolkitKt"))
    }
}

tasks.build {
    dependsOn(shadowJar)
}

tasks.jar {
    enabled = false
}

publishing {
    publications.withType<MavenPublication> {
        artifact(shadowJar)
    }
}

description = "Z PL/SQL Analyzer :: SSLR Toolkit"
