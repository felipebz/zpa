package com.felipebz.zpa

import com.diffplug.gradle.spotless.SpotlessExtension

plugins {
    id("com.diffplug.spotless")
}

configure<SpotlessExtension> {
    kotlin {
        target("src/**/*.kt")
        val lines = rootProject.file("LICENSE_HEADER").readLines().filter { it.isNotBlank() }
        licenseHeader(lines.joinToString("", prefix = "//\n", postfix = "//\n\n") { "// $it\n" })
    }
}

tasks.withType<Jar>().configureEach {
    into("META-INF") {
        from(rootProject.file("LICENSE"), rootProject.file("COPYING"))
    }
}
