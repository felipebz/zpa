package com.felipebz.zpa

import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.spotless.LineEnding

plugins {
    id("com.diffplug.spotless")
}

configure<SpotlessExtension> {
    // The default (Git attributes) loads JGit, which is pinned to the version JReleaser needs.
    lineEndings = LineEnding.PRESERVE
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
