import java.util.Date
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.ShadowExtension

plugins {
    id("com.github.johnrengelman.shadow") version Versions.plugin_shadow
}

dependencies {
    implementation(Libs.flr_core)
    implementation(project(":zpa-core"))
    implementation(project(":plsql-checks"))
    implementation(project(":plsql-checks-testkit"))
    compileOnly("org.sonarsource.sonarqube:sonar-plugin-api:${Versions.min_sonarqube_api}")
}

val shadowJar = tasks.named<ShadowJar>("shadowJar") {
    minimize {
        exclude(project(":plsql-checks-testkit"))
    }
    relocate("com.sonar.sslr.api", "org.sonar.plugins.plsqlopen.api.sslr")
    archiveClassifier.set("")
    manifest {
        val buildDate = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ").withZone(ZoneId.systemDefault()).format(Date().toInstant())
        attributes(mapOf(
            "Plugin-BuildDate" to buildDate,
            "Plugin-ChildFirstClassLoader" to "false",
            "Plugin-Class" to "org.sonar.plsqlopen.PlSqlPlugin",
            "Plugin-Description" to "Enables analysis and reporting on PL/SQL projects.",
            "Plugin-Developers" to "Felipe Zorzo",
            "Plugin-Display-Version" to project.version,
            "Plugin-Homepage" to "https://felipezorzo.com.br/zpa",
            "Plugin-IssueTrackerUrl" to "https://github.com/felipebz/zpa/issues",
            "Plugin-Key" to "plsqlopen",
            "Plugin-License" to "GNU LGPL 3",
            "Plugin-Name" to "Z PL/SQL Analyzer",
            "Plugin-Organization" to "Felipe Zorzo",
            "Plugin-OrganizationUrl" to "https://felipezorzo.com.br",
            "Plugin-SourcesUrl" to "https://github.com/felipebz/zpa",
            "Plugin-Version" to project.version,
            "Sonar-Version" to "7.6",
            "SonarLint-Supported" to "false"
        ))
    }
}

tasks.jar {
    enabled = false
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        //artifact(shadowJar)
        project.extensions.configure<ShadowExtension> {
            val publication = this@configureEach
            publication.pom.withXml { asNode().remove((asNode().get("dependencies") as groovy.util.NodeList).first() as groovy.util.Node) }
            component(this@configureEach)
        }
    }
    /*publications {
        create<MavenPublication>("shadow") {
            project.extensions.configure<com.github.jengelman.gradle.plugins.shadow.ShadowExtension>() {
                component(this@create)
            }
        }
    }*/
}

description = "Z PL/SQL Analyzer for SonarQube"
