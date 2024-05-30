import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("io.github.goooler.shadow") version Versions.plugin_shadow
}

dependencies {
    implementation(Libs.flr_xpath)
    implementation(Libs.flr_toolkit)
    implementation("com.formdev:flatlaf:3.4.1")
    implementation(project(":zpa-core"))
}

val shadowJar = tasks.named<ShadowJar>("shadowJar") {
    minimize {
        exclude(dependency("com.formdev:flatlaf"))
        exclude(dependency("jaxen:jaxen"))
    }
    archiveClassifier.set("")
    manifest {
        attributes(mapOf(
            "Main-Class" to "org.sonar.plsqlopen.toolkit.PlSqlToolkitKt",
            "Add-Opens" to "java.desktop/sun.awt.shell"
        ))
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
