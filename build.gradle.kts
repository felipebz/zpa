import com.hierynomus.gradle.license.tasks.LicenseCheck
import com.hierynomus.gradle.license.tasks.LicenseFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*

plugins {
    `java-library`
    `maven-publish`
    signing
    jacoco
    kotlin("jvm") version "1.6.10"
    id("org.jetbrains.dokka") version ("1.5.0")
    id("com.github.hierynomus.license") version "0.16.1"
    id("org.sonarqube") version "3.3"
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
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    group = "com.felipebz.zpa"
    version = "3.1.2-SNAPSHOT"
}

subprojects {
    apply(plugin = "com.github.hierynomus.license")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "jacoco")
    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "signing")

    dependencies {
        implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
        testImplementation(platform(Libs.junit_bom))
        testImplementation(Libs.junit_jupiter)
        testImplementation(Libs.assertj)
        testImplementation(Libs.mockito)
    }

    configurations {
        // include compileOnly dependencies during test
        testImplementation {
            extendsFrom(configurations.compileOnly.get())
        }
    }

    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            this.jvmTarget = "1.8"
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
            if (project.version.toString().endsWith("SNAPSHOT")) {
                maven {
                    name = "AzureArtifacts"
                    url = uri("https://pkgs.dev.azure.com/felipebz/z-plsql-analyzer/_packaging/public_feed/maven/v1")
                    credentials {
                        username = project.findProperty("gpr.user") as String? ?: System.getenv("DEPLOY_USERNAME")
                        password = project.findProperty("gpr.key") as String? ?: System.getenv("DEPLOY_TOKEN")
                    }
                }
            }
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

sonarqube {
    properties {
        property("sonar.projectName", "Z PL/SQL Analyzer")
    }
}
