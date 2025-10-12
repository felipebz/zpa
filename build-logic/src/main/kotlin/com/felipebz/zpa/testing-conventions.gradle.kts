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
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter(libs.findVersion("junit").get().requiredVersion)
        }
    }
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test) // tests are required to run before generating the report
}

jacoco {
    toolVersion = "0.8.14"
}

tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
    }
}
