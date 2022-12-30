import com.hierynomus.gradle.license.tasks.LicenseCheck
import com.hierynomus.gradle.license.tasks.LicenseFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*

plugins {
    `java-library`
    `maven-publish`
    signing
    jacoco
    kotlin("jvm") version "1.8.0"
    id("org.jetbrains.dokka") version ("1.7.20")
    id("com.github.hierynomus.license") version "0.16.1"
    id("org.sonarqube") version "3.5.0.2730"
    id("org.jreleaser") version "1.3.1"
}

allprojects {
    apply(plugin = "java")
    apply(plugin = "maven-publish")

    repositories {
        mavenCentral()
        mavenLocal()
        maven {
            setUrl("https://s01.oss.sonatype.org/content/repositories/snapshots/")
        }
    }

    java {
        withSourcesJar()
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(11))
        }
    }

    group = "com.felipebz.zpa"
    version = "3.3.0-SNAPSHOT"
}

subprojects {
    apply(plugin = "com.github.hierynomus.license")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "jacoco")
    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "signing")

    dependencies {
        implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
        testImplementation(Libs.assertj)
        testImplementation(Libs.mockito)
        testImplementation(Libs.mockito_kotlin)
    }

    configurations {
        // include compileOnly dependencies during test
        testImplementation {
            extendsFrom(configurations.compileOnly.get())
        }
    }

    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            this.jvmTarget = "11"
        }
    }

    testing {
        suites {
            configureEach {
                if (this is JvmTestSuite) {
                    useJUnitJupiter(Versions.junit)
                }
            }
        }
    }

    tasks.test {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
        finalizedBy(tasks.jacocoTestReport)
    }

    tasks.jacocoTestReport {
        dependsOn(tasks.test) // tests are required to run before generating the report
    }

    jacoco {
        toolVersion = "0.8.7"
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
        dependsOn(tasks.dokkaJavadoc)
        from(tasks.dokkaJavadoc.flatMap { it.outputDirectory })
        archiveClassifier.set("javadoc")
    }

    signing {
        setRequired({
            gradle.taskGraph.hasTask("publish")
        })
        sign(publishing.publications)
    }

    publishing {
        repositories {
            maven {
                val releaseRepo = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                val snapshotRepo = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
                url = if (project.version.toString().endsWith("SNAPSHOT")) snapshotRepo else releaseRepo
                credentials {
                    username = project.findProperty("ossrh.user") as String? ?: System.getenv("OSSRH_USERNAME")
                    password = project.findProperty("ossrh.password") as String? ?: System.getenv("OSSRH_PASSWORD")
                }
            }
        }
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
                artifact(dokka)
                pom {
                    name.set(provider { project.description })
                    description.set(provider { project.description })
                    url.set("https://felipezorzo.com.br/zpa")
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
        description.set("Z PL/SQL Analyzer")
        authors.set(listOf("felipebz"))
        license.set("LGPL-3.0")
        links {
            homepage.set("https://felipezorzo.com.br/zpa/")
        }
        inceptionYear.set("2015")
    }
    release {
        github {
            overwrite.set(true)
            tagName.set("{{projectVersion}}")
            changelog {
                formatted.set(org.jreleaser.model.Active.ALWAYS)
                preset.set("conventional-commits")
                format.set("- {{commitShortHash}} {{commitTitle}}")
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
    }
}

sonarqube {
    properties {
        property("sonar.projectName", "Z PL/SQL Analyzer")
    }
}
