import de.undercouch.gradle.tasks.download.Download

plugins {
    id("com.felipebz.zpa.build-conventions")
    alias(libs.plugins.download)
}

dependencies {
    implementation(libs.flr.core)
    implementation(libs.flr.xpath)
    implementation(project(":zpa-core"))
    testImplementation(project(path = ":zpa-checks-testkit", configuration = "rawRuntimeElements"))
}

testing {
    suites {
        register<JvmTestSuite>("integrationTest") {
            val downloadZipFile = tasks.register<Download>("downloadZipFile") {
                group = "build setup"
                description = "Downloads the SQLcl archive used by the integration tests."
                val sqlclVersion = "26.3.0.260.1620"
                src("https://download.oracle.com/otn_software/java/sqldeveloper/sqlcl-$sqlclVersion.zip")
                overwrite(false)
                dest(layout.projectDirectory.dir("tools").file("sqlcl-$sqlclVersion.zip"))
            }

            val downloadAndUnzipFile = tasks.register<Copy>("downloadAndUnzipFile") {
                group = "build setup"
                description = "Extracts the SQLcl archive into tools/ for the integration tests."
                dependsOn(downloadZipFile)
                from(zipTree(downloadZipFile.get().dest))
                into(layout.projectDirectory.dir("tools"))
            }

            tasks.named("compileIntegrationTestKotlin") {
                dependsOn(downloadAndUnzipFile)
            }

            tasks.named("integrationTest") {
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
