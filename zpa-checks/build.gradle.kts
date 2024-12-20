import de.undercouch.gradle.tasks.download.Download

plugins {
    alias(libs.plugins.download)
}

dependencies {
    implementation(libs.flr.core)
    implementation(libs.flr.xpath)
    implementation(project(":zpa-core"))
    testImplementation(project(":zpa-checks-testkit"))
}

testing {
    suites {
        register<JvmTestSuite>("integrationTest") {
            testType.set(TestSuiteType.INTEGRATION_TEST)

            val downloadZipFile by tasks.creating(Download::class) {
                val sqlclVersion = "24.3.0.285.0530"
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
                implementation(libs.jackson)
                implementation(project(":zpa-core"))
                implementation(libs.jsoup)
            }
        }
    }
}

description = "ZPA Checks"
