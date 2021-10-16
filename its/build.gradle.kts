allprojects {
    tasks.withType<PublishToMavenRepository>().configureEach {
        onlyIf { false }
    }
}
