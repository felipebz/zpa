package com.felipebz.zpa

import com.hierynomus.gradle.license.tasks.LicenseCheck
import com.hierynomus.gradle.license.tasks.LicenseFormat
import java.io.File
import java.util.*

plugins {
    id("com.github.hierynomus.license")
}

tasks.withType<LicenseFormat>().configureEach {
    extra.set("year", Calendar.getInstance().get(Calendar.YEAR))
    header = File(this.project.rootDir, "LICENSE_HEADER")
    include("**/*.kt")
}

tasks.withType<LicenseCheck>().configureEach {
    extra.set("year", Calendar.getInstance().get(Calendar.YEAR))
    header = File(this.project.rootDir, "LICENSE_HEADER")
    include("**/*.kt")
}
