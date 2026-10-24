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
    // Optional Oracle metadata acquisition; the Oracle JDBC driver itself is never bundled (users supply it).
    implementation(project(":zpa-oracle-metadata"))
    compileOnly(libs.sonar.plugin.api)
    compileOnly(libs.sonar.plugin.api.impl)
}

// The Oracle JDBC driver used by the database-backed tests. It is passed to them as a JAR path and kept off their class
// path, so that they load it as users do (sonar.zpa.oracle.driverPath); it is never part of the plugin.
val oracleJdbcDriver by configurations.creating {
    isTransitive = false
}

// A test-only custom-rules plugin whose check consumes the external metadata, compiled against the relocated API of the
// shaded plugin JAR exactly as third-party rule plugins are.
val oracleProbePlugin by sourceSets.creating

dependencies {
    oracleJdbcDriver(libs.ojdbc)
    "oracleProbePluginCompileOnly"(libs.sonar.plugin.api)
    "oracleProbePluginCompileOnly"(files(tasks.named("shadowJar")))
}

val oracleProbePluginJar = tasks.register<Jar>("oracleProbePluginJar") {
    group = "build"
    description = "Packages the plugin used by the Oracle metadata probe."
    from(oracleProbePlugin.output)
    archiveBaseName.set("zpa-oracle-probe-plugin")
    destinationDirectory.set(layout.buildDirectory.dir("oracle-probe-plugin"))
    manifest {
        attributes(mapOf(
            "Plugin-ChildFirstClassLoader" to "false",
            "Plugin-Class" to "com.felipebz.zpa.probe.OracleProbePlugin",
            "Plugin-Description" to "Test only",
            "Plugin-Display-Version" to project.version,
            "Plugin-Key" to "zpaoracleprobe",
            "Plugin-Name" to "ZPA Oracle metadata probe (test only)",
            "Plugin-Version" to project.version,
            "Sonar-Version" to libs.versions.min.sonarqubeapi,
            "SonarLint-Supported" to "false",
            "Plugin-RequiredForLanguages" to "plsqlopen"
        ))
    }
}

testing {
    suites {
        // Database-backed tests: never part of `check`/`build`; they need an Oracle instance (see README.md).
        register<JvmTestSuite>("oracleIntegrationTest") {
            useJUnitJupiter(libs.versions.junit.get())
            dependencies {
                implementation(files(sourceSets.main.get().output, sourceSets.test.get().output))
                implementation(libs.sonar.ws)
                implementation(libs.sonar.orchestrator)
            }
            targets.all {
                testTask.configure {
                    description = "Runs the Oracle database integration tests (requires ZPA_ORACLE_* environment variables)."
                    val shadow = tasks.named<ShadowJar>("shadowJar")
                    dependsOn(shadow, oracleProbePluginJar)
                    val launcher = javaToolchains.launcherFor {
                        languageVersion.set(JavaLanguageVersion.of(21))
                    }
                    javaLauncher.set(launcher)
                    environment("JAVA_HOME", launcher.get().metadata.installationPath.asFile.toString())
                    systemProperty("java.awt.headless", "true")
                    systemProperty("zpa.oracle.driverJar", oracleJdbcDriver.singleFile.absolutePath)
                    systemProperty("zpa.plugin.jar", shadow.get().archiveFile.get().asFile.absolutePath)
                    systemProperty("zpa.probePlugin.jar", oracleProbePluginJar.get().archiveFile.get().asFile.absolutePath)
                    System.getProperty("sonar.zipFile")?.let { systemProperty("sonar.zipFile", it) }
                    outputs.upToDateWhen { false }
                    testLogging {
                        showStandardStreams = true
                    }
                }
            }
        }
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
                            languageVersion.set(JavaLanguageVersion.of(21))
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

configurations["oracleIntegrationTestImplementation"].extendsFrom(configurations.testImplementation.get())
configurations["oracleIntegrationTestRuntimeOnly"].extendsFrom(configurations.testRuntimeOnly.get())

// The database-backed tests use the sensor's internal test hooks (environment, acquirer), as the unit tests do.
kotlin.target.compilations.named("oracleIntegrationTest") {
    associateWith(kotlin.target.compilations.getByName("main"))
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
