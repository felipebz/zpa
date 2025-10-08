import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import groovy.util.Node
import groovy.util.NodeList
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

plugins {
    id("com.felipebz.zpa.build-conventions")
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(libs.flr.core)
    implementation(libs.jackson.xml)
    implementation(libs.woodstox)
    implementation(project(":zpa-core"))
    implementation(project(":zpa-checks"))
    implementation(project(":zpa-checks-testkit"))
    compileOnly(libs.sonar.plugin.api)
    compileOnly(libs.sonar.plugin.api.impl)
}

testing {
    suites {
        register<JvmTestSuite>("integrationTest") {
            dependencies {
                implementation(libs.assertj)
                implementation(libs.sonar.ws)
                implementation(libs.sonar.orchestrator)
            }

            targets {
                all {
                    testTask.configure {
                        val zipFile = System.getProperty("sonar.zipFile")

                        val launcher = javaToolchains.launcherFor {
                            languageVersion.set(JavaLanguageVersion.of(17))
                        }
                        javaLauncher.set(launcher)

                        environment("JAVA_HOME", launcher.get().metadata.installationPath.asFile.toString())
                        systemProperty("java.awt.headless", "true")
                        if (zipFile != null) {
                            systemProperty("sonar.zipFile", zipFile)
                        }
                        outputs.upToDateWhen { false }
                    }
                }
            }
        }
    }
}

val shadowJar = tasks.named<ShadowJar>("shadowJar") {
    relocate("com.felipebz.flr.api", "org.sonar.plugins.plsqlopen.api.sslr")
    relocate("com.felipebz.zpa.api", "org.sonar.plugins.plsqlopen.api")
    archiveClassifier.set("")
    manifest {
        val buildDate = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ").withZone(ZoneId.systemDefault()).format(Date().toInstant())
        attributes(mapOf(
            "Plugin-BuildDate" to buildDate,
            "Plugin-ChildFirstClassLoader" to "false",
            "Plugin-Class" to "com.felipebz.zpa.PlSqlPlugin",
            "Plugin-Description" to "Enables analysis and reporting on PL/SQL projects.",
            "Plugin-Developers" to "Felipe Zorzo",
            "Plugin-Display-Version" to project.version,
            "Plugin-Homepage" to "https://zpa.felipebz.com",
            "Plugin-IssueTrackerUrl" to "https://github.com/felipebz/zpa/issues",
            "Plugin-Key" to "plsqlopen",
            "Plugin-License" to "GNU LGPL 3",
            "Plugin-Name" to "ZPA Plugin for SonarQube",
            "Plugin-Organization" to "Felipe Zorzo",
            "Plugin-OrganizationUrl" to "https://felipezorzo.com.br",
            "Plugin-SourcesUrl" to "https://github.com/felipebz/zpa",
            "Plugin-Version" to project.version,
            "Sonar-Version" to libs.versions.min.sonarqubeapi,
            "SonarLint-Supported" to "false",
            "Plugin-RequiredForLanguages" to "plsqlopen"
        ))
    }
}

tasks.build {
    dependsOn(shadowJar)
}

tasks.jar {
    enabled = false
}

// Disable Gradle module metadata as it lists wrong dependencies
tasks.withType<GenerateModuleMetadata> {
    enabled = false
}

publishing {
    publications.withType<MavenPublication> {
        this.pom.withXml {
            val pomNode = asNode()

            val dependencyNodes = pomNode.get("dependencies") as NodeList
            dependencyNodes.forEach {
                (it as Node).parent().remove(it)
            }
        }
        artifact(shadowJar)
    }
}

description = "ZPA Plugin for SonarQube"
