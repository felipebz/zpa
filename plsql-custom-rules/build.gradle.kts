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
        url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    }
}

val minSonarQubeVersion = "7.6"

dependencies {
    compileOnly("org.sonarsource.sonarqube:sonar-plugin-api:$minSonarQubeVersion")
    compileOnly("com.felipebz.zpa:sonar-zpa-plugin:3.1.0")
    testImplementation("com.felipebz.zpa:zpa-checks-testkit:3.1.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.0")
}

configurations {
    // include compileOnly dependencies during test
    testImplementation {
        extendsFrom(configurations.compileOnly.get())
    }
}

tasks.test {
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
            "Sonar-Version" to minSonarQubeVersion,
            "SonarLint-Supported" to "false"
        ))
    }
}

group = "com.company"
version = "1.0-SNAPSHOT"
description = "Company PL/SQL Rules"
java.sourceCompatibility = JavaVersion.VERSION_1_8
java.targetCompatibility = JavaVersion.VERSION_1_8

tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
}
