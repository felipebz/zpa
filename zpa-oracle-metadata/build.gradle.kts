plugins {
    id("com.felipebz.zpa.build-conventions")
}

dependencies {
    api(project(":zpa-core"))
}

testing {
    suites {
        // Database-backed tests: never part of `check`/`build`; they need an Oracle instance (see README.md).
        register<JvmTestSuite>("oracleIntegrationTest") {
            useJUnitJupiter(libs.versions.junit.get())
            dependencies {
                implementation(project())
                implementation(libs.assertj)
                implementation(libs.ojdbc)
            }
            targets.all {
                testTask.configure {
                    description = "Runs the Oracle database integration tests (requires ZPA_ORACLE_* environment variables)."
                    outputs.upToDateWhen { false }
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
