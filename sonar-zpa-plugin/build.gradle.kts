import com.github.jengelman.gradle.plugins.shadow.ShadowExtension
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import groovy.util.Node
import groovy.util.NodeList
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

plugins {
    id("com.github.johnrengelman.shadow") version Versions.plugin_shadow
}

dependencies {
    implementation(Libs.flr_core)
    implementation(project(":zpa-core"))
    implementation(project(":zpa-checks"))
    implementation(project(":zpa-checks-testkit"))
    compileOnly("org.sonarsource.api.plugin:sonar-plugin-api:${Versions.min_sonarqube_api}")
    testImplementation("org.sonarsource.sonarqube:sonar-plugin-api-impl:${Versions.min_sonarqube}")
}

testing {
    suites {
        val integrationTest by registering(JvmTestSuite::class) {
            testType.set(TestSuiteType.INTEGRATION_TEST)

            dependencies {
                implementation(Libs.assertj)
                implementation("org.sonarsource.sonarqube:sonar-ws:${Versions.min_sonarqube}")
                implementation("org.sonarsource.orchestrator:sonar-orchestrator-junit5:${Versions.sonarqube_orchestrator}")
            }

            targets {
                all {
                    testTask.configure {
                        val runtimeVersion = System.getProperty("sonar.runtimeVersion", "LATEST_RELEASE[9.9]")

                        javaLauncher.set(javaToolchains.launcherFor {
                            languageVersion.set(JavaLanguageVersion.of(17))
                        })

                        systemProperty("java.awt.headless", "true")
                        systemProperty("sonar.runtimeVersion", runtimeVersion)
                        outputs.upToDateWhen { false }
                    }
                }
            }
        }
    }
}

val shadowJar = tasks.named<ShadowJar>("shadowJar") {
    relocate("com.felipebz.flr.api", "org.sonar.plugins.plsqlopen.api.sslr")
    archiveClassifier.set("")
    manifest {
        val buildDate = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ").withZone(ZoneId.systemDefault()).format(Date().toInstant())
        attributes(mapOf(
            "Plugin-BuildDate" to buildDate,
            "Plugin-ChildFirstClassLoader" to "false",
            "Plugin-Class" to "org.sonar.plsqlopen.PlSqlPlugin",
            "Plugin-Description" to "Enables analysis and reporting on PL/SQL projects.",
            "Plugin-Developers" to "Felipe Zorzo",
            "Plugin-Display-Version" to project.version,
            "Plugin-Homepage" to "https://felipezorzo.com.br/zpa",
            "Plugin-IssueTrackerUrl" to "https://github.com/felipebz/zpa/issues",
            "Plugin-Key" to "plsqlopen",
            "Plugin-License" to "GNU LGPL 3",
            "Plugin-Name" to "Z PL/SQL Analyzer",
            "Plugin-Organization" to "Felipe Zorzo",
            "Plugin-OrganizationUrl" to "https://felipezorzo.com.br",
            "Plugin-SourcesUrl" to "https://github.com/felipebz/zpa",
            "Plugin-Version" to project.version,
            "Sonar-Version" to Versions.min_sonarqube,
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
    publications.withType<MavenPublication>().configureEach {
        project.extensions.configure<ShadowExtension> {
            val publication = this@configureEach
            publication.pom.withXml {
                val pomNode = asNode()

                val dependencyNodes = pomNode.get("dependencies") as NodeList
                dependencyNodes.forEach {
                    (it as Node).parent().remove(it)
                }
            }
            component(this@configureEach)
        }
    }
}

description = "Z PL/SQL Analyzer for SonarQube"
