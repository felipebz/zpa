dependencies {
    testImplementation(project(":zpa-core"))
    testImplementation(project(":plsql-checks"))
    testImplementation(Libs.gson)
    testImplementation("org.jsoup:jsoup:${Versions.jsoup}")
}

tasks.test {
    onlyIf {
        project.hasProperty("it") || System.getProperty("idea.active") != null
    }
}

description = "PL/SQL ITs :: Ruling"
