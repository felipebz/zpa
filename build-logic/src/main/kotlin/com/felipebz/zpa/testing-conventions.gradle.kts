package com.felipebz.zpa

plugins {
    id("com.felipebz.zpa.kotlin-conventions")
    id("jacoco")
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

dependencies {
    testImplementation(libs.findLibrary("assertj").get())
    testImplementation(libs.findLibrary("mockito").get())
    testImplementation(libs.findLibrary("mockito-kotlin").get())
}

configurations {
    // include compileOnly dependencies during test
    testImplementation {
        extendsFrom(configurations.compileOnly.get())
    }
}

testing {
    suites {
        getByName<JvmTestSuite>("test") {
            useJUnitJupiter(libs.findVersion("junit").get().requiredVersion)
        }
    }
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}

// zpa's own test runs, the ruling included, fail on a file the local pass cannot analyze instead of logging it.
tasks.withType<Test>().configureEach {
    systemProperty("com.felipebz.zpa.internal.failOnLocalPassError", "true")
}

tasks.jacocoTestReport {
    dependsOn(tasks.test) // tests are required to run before generating the report
}

jacoco {
    toolVersion = "0.8.15"
}

tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
    }
}
