plugins {
    id("com.felipebz.zpa.build-conventions")
}

dependencies {
    api(libs.flr.core)
    implementation(libs.jackson)
    testImplementation(libs.flr.testing.harness)
}

description = "ZPA Core"
