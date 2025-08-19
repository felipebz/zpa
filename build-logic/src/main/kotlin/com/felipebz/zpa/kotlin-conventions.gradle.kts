package com.felipebz.zpa

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm")
}

repositories {
    mavenCentral()
    mavenLocal()
    maven {
        setUrl("https://central.sonatype.com/repository/maven-snapshots/")
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs = listOf("-Xconsistent-data-class-copy-visibility")
    }
}

java {
    withSourcesJar()
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}
