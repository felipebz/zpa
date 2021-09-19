dependencies {
    testImplementation("org.sonarsource.sonarqube:sonar-ws:${Versions.min_sonarqube_api}")
    testImplementation("org.sonarsource.orchestrator:sonar-orchestrator:${Versions.sonarqube_orchestrator}")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine")
}

tasks.test {
    onlyIf {
        project.hasProperty("it")
    }
    filter {
        includeTestsMatching("org.sonar.plsqlopen.it.Tests")
    }
    systemProperty("java.awt.headless", "true")
    outputs.upToDateWhen { false }
}

description = "PL/SQL ITs :: Plugin"
