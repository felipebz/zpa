import de.undercouch.gradle.tasks.download.Download

plugins {
    id("de.undercouch.download") version "5.6.0"
}

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

            val downloadZipFile by tasks.creating(Download::class) {
                val sqlclVersion = "24.1.0.087.0929"
                src("https://download.oracle.com/otn_software/java/sqldeveloper/sqlcl-$sqlclVersion.zip")
                overwrite(false)
                dest(layout.projectDirectory.dir("tools").file("sqlcl-$sqlclVersion.zip"))
            }

            val downloadAndUnzipFile = tasks.register<Copy>("downloadAndUnzipFile") {
                dependsOn(downloadZipFile)
                from(zipTree(downloadZipFile.dest))
                into(layout.projectDirectory.dir("tools"))
            }

            tasks.build {
                dependsOn(downloadAndUnzipFile)
            }

            dependencies {
                implementation(layout.projectDirectory.dir("tools/sqlcl/lib").asFileTree)
                implementation(project())
                implementation(Libs.jackson)
                implementation(project(":zpa-core"))
                implementation("org.jsoup:jsoup:${Versions.jsoup}")
            }
        }
    }
}

description = "Z PL/SQL Analyzer :: Checks"
