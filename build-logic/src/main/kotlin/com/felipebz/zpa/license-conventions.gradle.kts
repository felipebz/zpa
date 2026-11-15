package com.felipebz.zpa

import com.diffplug.gradle.spotless.KotlinExtension
import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.spotless.LineEnding

plugins {
    id("com.diffplug.spotless")
}

configure<SpotlessExtension> {
    // The default (Git attributes) loads JGit, which is pinned to the version JReleaser needs.
    lineEndings = LineEnding.PRESERVE
    val lines = rootProject.file("LICENSE_HEADER").readLines().filter { it.isNotBlank() }
    val license = lines.filterNot { it.startsWith("SPDX-FileCopyrightText:") }
    fun header(copyright: List<String>) = (copyright + license).joinToString("", prefix = "//\n", postfix = "//\n\n") { "// $it\n" }
    val sources = fileTree("src") { include("**/*.kt") }
    val sslrDerived = sources.filter { file ->
        file.useLines { text -> text.takeWhile { !it.startsWith("package ") }.any { "SonarSource SA" in it } }
    }
    kotlin {
        target(sources.minus(sslrDerived))
        licenseHeader(header(lines - license.toSet()))
    }
    format("kotlinDerivedFromSslr", KotlinExtension::class.java) {
        target(sslrDerived)
        licenseHeader(header(listOf("SPDX-FileCopyrightText: 2010-2021 SonarSource SA", "SPDX-FileCopyrightText: 2021 Felipe Zorzo")))
    }
}

tasks.withType<Jar>().configureEach {
    into("META-INF") {
        from(rootProject.file("LICENSE"), rootProject.file("COPYING"))
    }
}
