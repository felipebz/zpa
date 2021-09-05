dependencies {
    testImplementation(project(":zpa-core"))
    testImplementation(project(":plsql-checks"))
    testImplementation(Libs.gson)
    testImplementation("org.jsoup:jsoup:${Versions.jsoup}")
}

tasks.test {
    onlyIf {
        project.hasProperty("it")
    }
}

description = "PL/SQL ITs :: Ruling"
