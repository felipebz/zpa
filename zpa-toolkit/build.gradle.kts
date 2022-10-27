import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.hierynomus.gradle.license.tasks.LicenseCheck
import com.hierynomus.gradle.license.tasks.LicenseFormat

plugins {
    id("com.github.johnrengelman.shadow") version Versions.plugin_shadow
}

dependencies {
    implementation(Libs.flr_xpath)
    implementation(Libs.flr_toolkit)
    implementation("com.formdev:flatlaf:2.2")
    implementation(project(":zpa-core"))
}

val shadowJar = tasks.named<ShadowJar>("shadowJar") {
    minimize {
        exclude(dependency("com.formdev:flatlaf"))
        exclude(dependency("jaxen:jaxen"))
    }
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

tasks.withType<LicenseFormat>().configureEach {
    skipExistingHeaders = true
}

tasks.withType<LicenseCheck>().configureEach {
    skipExistingHeaders = true
}

description = "Z PL/SQL Analyzer :: SSLR Toolkit"
