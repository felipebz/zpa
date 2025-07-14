import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.felipebz.zpa.build-conventions")
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(libs.flr.xpath)
    implementation(libs.flr.toolkit)
    implementation(libs.flatlaf)
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
            "Main-Class" to "com.felipebz.zpa.toolkit.ZpaToolkitKt",
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

description = "ZPA Toolkit"
