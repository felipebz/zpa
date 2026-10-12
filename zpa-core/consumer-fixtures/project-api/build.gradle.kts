// Independent consumer of the packaged zpa-core artifact. This is a normal, non-published subproject:
// it only applies the Kotlin/testing conventions (no license, publishing or JReleaser conventions).
plugins {
    id("com.felipebz.zpa.kotlin-conventions")
    id("com.felipebz.zpa.testing-conventions")
}

dependencies {
    testImplementation(project(":zpa-core"))
}

// Consumer code must see the packaged JAR, not the producer's class directories.
listOf(configurations.testCompileClasspath, configurations.testRuntimeClasspath).forEach { configuration ->
    configuration.configure {
        attributes {
            attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
        }
    }
}

val verifyPackagedZpaCore = tasks.register("verifyPackagedZpaCore") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Fails unless consumer compilation and runtime use the packaged zpa-core JAR."
    val compileClasspath = configurations.testCompileClasspath.get()
    val runtimeClasspath = configurations.testRuntimeClasspath.get()
    // Run the tasks that produce the classpath artifacts (e.g. the zpa-core JAR) first.
    dependsOn(compileClasspath, runtimeClasspath)
    val producerClasses = project(":zpa-core").layout.buildDirectory.dir("classes")
    doLast {
        val classesRoot = producerClasses.get().asFile
        listOf("test compile" to compileClasspath, "test runtime" to runtimeClasspath).forEach { (name, files) ->
            val entries = files.files
            val zpaCore = entries.filter { it.name.startsWith("zpa-core-") && it.extension == "jar" }
            check(zpaCore.size == 1) { "Expected exactly one packaged zpa-core jar on the $name classpath, got $zpaCore" }
            check(entries.none { it.isDirectory && it.startsWith(classesRoot) }) {
                "The $name classpath must not contain zpa-core class directories"
            }
            logger.info("{} classpath uses {}", name, zpaCore.single())
        }
    }
}

tasks.test {
    dependsOn(verifyPackagedZpaCore)
}
