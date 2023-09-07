object Versions {
    const val junit = "5.9.3"
    const val assertj = "3.24.2"
    const val mockito = "5.4.0"
    const val mockito_kotlin = "5.0.0"
    const val flr = "1.3.0-SNAPSHOT"
    const val gson = "2.10.1"
    const val jsoup = "1.16.1"
    const val min_sonarqube = "9.9.0.65466"
    const val min_sonarqube_api = "9.14.0.375"
    const val sonarqube_orchestrator = "4.0.0.404"

    const val plugin_shadow = "7.1.2"
}

object Libs {
    const val assertj = "org.assertj:assertj-core:${Versions.assertj}"
    const val mockito = "org.mockito:mockito-core:${Versions.mockito}"
    const val mockito_kotlin = "org.mockito.kotlin:mockito-kotlin:${Versions.mockito_kotlin}"
    const val flr_core = "com.felipebz.flr:flr-core:${Versions.flr}"
    const val flr_xpath = "com.felipebz.flr:flr-xpath:${Versions.flr}"
    const val flr_toolkit = "com.felipebz.flr:flr-toolkit:${Versions.flr}"
    const val flr_testing_harness = "com.felipebz.flr:flr-testing-harness:${Versions.flr}"
    const val gson = "com.google.code.gson:gson:${Versions.gson}"
}
