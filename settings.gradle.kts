pluginManagement {
    includeBuild("build-logic")
}

rootProject.name = "zpa"
include(":sonar-zpa-plugin")
include(":zpa-checks")
include(":zpa-checks-testkit")
include(":zpa-core")
include(":zpa-toolkit")
