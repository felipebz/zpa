package com.felipebz.zpa

plugins {
    id("org.sonarqube")
}

sonarqube {
    properties {
        property("sonar.projectName", "ZPA")
    }
}
