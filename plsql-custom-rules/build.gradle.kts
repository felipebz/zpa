import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

plugins {
    java
}

repositories {
    mavenCentral()
    mavenLocal()
    maven {
        url = uri("https://central.sonatype.com/repository/maven-snapshots/")
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

val minSonarQubeVersion = "25.8.0.111929"
val minSonarQubeApiVersion = "13.0.0.3026"
val junitVersion = "6.0.1"
val zpaVersion = "4.1.0-SNAPSHOT"

dependencies {
    compileOnly("org.sonarsource.api.plugin:sonar-plugin-api:$minSonarQubeApiVersion")
    compileOnly("com.felipebz.zpa:sonar-zpa-plugin:$zpaVersion")
    testImplementation("org.sonarsource.sonarqube:sonar-plugin-api-impl:$minSonarQubeVersion")
    testImplementation("com.felipebz.zpa:zpa-checks-testkit:$zpaVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

configurations {
    // include compileOnly dependencies during test
    testImplementation {
        extendsFrom(configurations.compileOnly.get())
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.jar {
    manifest {
        val buildDate = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ").withZone(ZoneId.systemDefault()).format(
            Date().toInstant())
        attributes(mapOf(
            "Plugin-BuildDate" to buildDate,
            "Plugin-ChildFirstClassLoader" to "false",
            "Plugin-Class" to "com.company.plsql.PlSqlCustomRulesPlugin",
            "Plugin-Description" to "PL/SQL Custom Rules",
            "Plugin-Developers" to "",
            "Plugin-Display-Version" to project.version,
            "Plugin-Key" to "myrules",
            "Plugin-License" to "",
            "Plugin-Name" to "Company PL/SQL Rules",
            "Plugin-Version" to project.version,
            "Sonar-Version" to minSonarQubeApiVersion,
            "SonarLint-Supported" to "false",
            "Plugin-RequiredForLanguages" to "plsqlopen"
        ))
    }
}

group = "com.company"
version = "1.0-SNAPSHOT"
description = "Company PL/SQL Rules"

tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
}
