pluginManagement {
    includeBuild("build-logic")
}

rootProject.name = "zpa"
include(":sonar-zpa-plugin")
include(":zpa-checks")
include(":zpa-checks-testkit")
include(":zpa-core")
include(":zpa-toolkit")
include(":project-api-consumer-test")
project(":project-api-consumer-test").projectDir = file("zpa-core/consumer-fixtures/project-api")
