dependencies {
    implementation(Libs.flr_core)
    implementation(Libs.flr_xpath)
    implementation(project(":zpa-core"))
    testImplementation(project(":zpa-checks-testkit"))
}

testing {
    suites {
        val integrationTest by registering(JvmTestSuite::class) {
            testType.set(TestSuiteType.INTEGRATION_TEST)

            dependencies {
                implementation(project())
                implementation(project(":zpa-core"))
                implementation(Libs.gson)
                implementation("org.jsoup:jsoup:${Versions.jsoup}")
            }
        }
    }
}

description = "Z PL/SQL Analyzer :: Checks"
