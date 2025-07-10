import com.hierynomus.gradle.license.tasks.LicenseCheck
import com.hierynomus.gradle.license.tasks.LicenseFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.*

plugins {
    `java-library`
    `maven-publish`
    signing
    jacoco
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.dokka.javadoc)
    alias(libs.plugins.license)
    alias(libs.plugins.sonarqube)
    alias(libs.plugins.jreleaser)
}

allprojects {
    apply(plugin = "java")
    apply(plugin = "maven-publish")

    repositories {
        mavenCentral()
        mavenLocal()
        maven {
            setUrl("https://central.sonatype.com/service/rest/repository/browse/maven-snapshots/")
        }
    }

    java {
        withSourcesJar()
    }

    group = "com.felipebz.zpa"
    version = "3.9.0-SNAPSHOT"
}

subprojects {
    apply(plugin = "com.github.hierynomus.license")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "jacoco")
    apply(plugin = "org.jetbrains.dokka-javadoc")
    apply(plugin = "signing")

    dependencies {
        testImplementation(rootProject.libs.assertj)
        testImplementation(rootProject.libs.mockito)
        testImplementation(rootProject.libs.mockito.kotlin)
    }

    configurations {
        // include compileOnly dependencies during test
        testImplementation {
            extendsFrom(configurations.compileOnly.get())
        }
    }

    kotlin {
        jvmToolchain(17)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            freeCompilerArgs = listOf("-Xconsistent-data-class-copy-visibility")
        }
    }

    testing {
        suites {
            val test by getting(JvmTestSuite::class) {
                useJUnitJupiter(rootProject.libs.versions.junit)
            }
        }
    }

    tasks.test {
        finalizedBy(tasks.jacocoTestReport)
    }

    tasks.jacocoTestReport {
        dependsOn(tasks.test) // tests are required to run before generating the report
    }

    jacoco {
        toolVersion = "0.8.13"
    }

    tasks.jacocoTestReport {
        reports {
            xml.required.set(true)
        }
    }

    tasks.withType<LicenseFormat>().configureEach {
        extra.set("year", Calendar.getInstance().get(Calendar.YEAR))
        header = File(this.project.rootDir, "LICENSE_HEADER")
        include("**/*.kt")
    }

    tasks.withType<LicenseCheck>().configureEach {
        extra.set("year", Calendar.getInstance().get(Calendar.YEAR))
        header = File(this.project.rootDir, "LICENSE_HEADER")
        include("**/*.kt")
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
}

jreleaser {
    project {
        description.set("ZPA - Parser and static code analysis tool for PL/SQL and Oracle SQL")
        authors.set(listOf("felipebz"))
        license.set("LGPL-3.0")
        links {
            homepage.set("https://zpa.felipebz.com")
        }
        inceptionYear.set("2015")
        snapshot {
            fullChangelog.set(true)
        }
    }
    release {
        github {
            overwrite.set(true)
            tagName.set("{{projectVersion}}")
            draft.set(true)
            changelog {
                formatted.set(org.jreleaser.model.Active.ALWAYS)
                preset.set("conventional-commits")
                contentTemplate.set(file("template/changelog.tpl"))
                contributors {
                    enabled.set(false)
                }
                hide {
                    uncategorized.set(true)
                }
            }
        }
    }
    distributions {
        listOf("sonar-zpa-plugin", "zpa-toolkit").forEach {
            create(it) {
                artifact {
                    path.set(file("{{distributionName}}/build/libs/{{distributionName}}-{{projectVersion}}.jar"))
                }
            }
        }
        create("plsql-custom-rules") {
            artifact {
                path.set(file("build/{{distributionName}}.zip"))
            }
        }
    }
    signing {
        active.set(org.jreleaser.model.Active.ALWAYS)
        armored.set(true)
    }
    deploy {
        maven {
            mavenCentral {
                create("sonatype") {
                    active.set(org.jreleaser.model.Active.RELEASE)
                    snapshotSupported.set(true)
                    url.set("https://central.sonatype.com/api/v1/publisher")
                    stagingRepository("build/staging-deploy")
                }
            }
            nexus2 {
                create("snapshot-deploy") {
                    active.set(org.jreleaser.model.Active.SNAPSHOT)
                    url.set("https://central.sonatype.com/repository/maven-snapshots")
                    snapshotUrl.set("https://central.sonatype.com/repository/maven-snapshots")
                    applyMavenCentralRules.set(true)
                    snapshotSupported.set(true)
                    closeRepository.set(true)
                    releaseRepository.set(true)
                    stagingRepository("build/staging-deploy")
                }
            }
        }
    }
}

sonarqube {
    properties {
        property("sonar.projectName", "ZPA")
    }
}
