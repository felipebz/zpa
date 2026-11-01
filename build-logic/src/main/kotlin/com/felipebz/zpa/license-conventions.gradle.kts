package com.felipebz.zpa

import com.diffplug.gradle.spotless.SpotlessExtension

plugins {
    id("com.diffplug.spotless")
}

configure<SpotlessExtension> {
    kotlin {
        target("src/**/*.kt")
        val lines = rootProject.file("LICENSE_HEADER").readLines().map { it.replace("\${year}", "\$YEAR") }
        licenseHeader(lines.joinToString("", prefix = "/**\n", postfix = " */\n") { if (it.isBlank()) " *\n" else " * $it\n" })
    }
}
