plugins {
    id("com.felipebz.zpa.build-conventions")
}

dependencies {
    api(project(":zpa-core"))
}

testing {
    suites {
        // Database-backed tests: never part of `check`/`build`. They start an Oracle container (Testcontainers) or use
        // the database named by ZPA_ORACLE_* environment variables (see README.md).
        register<JvmTestSuite>("oracleIntegrationTest") {
            useJUnitJupiter(libs.versions.junit.get())
            dependencies {
                implementation(project())
                implementation(libs.assertj)
                implementation(libs.ojdbc)
                implementation(libs.testcontainers.oracle.free)
            }
            targets.all {
                testTask.configure {
                    description = "Runs the Oracle database integration tests (needs Docker, or ZPA_ORACLE_* environment variables)."
                    outputs.upToDateWhen { false }
                    systemProperty("zpa.oracle.image", providers.gradleProperty("zpaOracleImage").get())
                    testLogging {
                        showStandardStreams = true
                    }
                }
            }
        }
    }
}

kotlin.target.compilations.named("oracleIntegrationTest") {
    associateWith(kotlin.target.compilations.getByName("main"))
}

description = "ZPA Oracle Metadata"
