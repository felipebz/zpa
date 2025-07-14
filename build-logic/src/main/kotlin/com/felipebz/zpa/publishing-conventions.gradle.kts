package com.felipebz.zpa

import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.register

plugins {
    id("maven-publish")
    id("org.jetbrains.dokka-javadoc")
    id("signing")
}

val dokka by tasks.register<Jar>("dokka") {
    dependsOn(tasks.dokkaGenerateModuleJavadoc)
    from(tasks.dokkaGenerateModuleJavadoc.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
}

signing {
    setRequired({
        gradle.taskGraph.hasTask("publish")
    })
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications)
}

publishing {
    repositories {
        maven {
            url = rootProject.layout.buildDirectory.dir("staging-deploy").get().asFile.toURI()
        }
    }
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifact(dokka)
            pom {
                name.set(provider { project.description })
                description.set(provider { project.description })
                url.set("https://zpa.felipebz.com")
                organization {
                    name.set("Felipe Zorzo")
                    url.set("https://felipezorzo.com.br")
                }
                licenses {
                    license {
                        name.set("GNU LGPL 3")
                        url.set("https://www.gnu.org/licenses/lgpl.txt")
                        distribution.set("repo")
                    }
                }
                scm {
                    url.set("https://github.com/felipebz/zpa")
                    connection.set("scm:git:https://github.com/felipebz/zpa.git")
                    developerConnection.set("scm:git:https://github.com/felipebz/zpa.git")
                }
                developers {
                    developer {
                        id.set("felipebz")
                        name.set("Felipe Zorzo")
                        url.set("https://felipezorzo.com.br")
                        email.set("felipe@felipezorzo.com.br")
                    }
                }
            }
        }
    }
}
