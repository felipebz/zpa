object Versions {
    const val junit = "5.8.1"
    const val assertj = "3.21.0"
    const val mockito = "4.0.0"
    const val mockito_kotlin = "4.0.0"
    const val flr = "1.1.0"
    const val gson = "2.8.8"
    const val jsoup = "1.14.3"
    const val min_sonarqube_api = "8.9.0.43852"
    const val sonarqube_orchestrator = "3.35.1.2719"

    const val plugin_shadow = "7.0.0"
}

object Libs {
    const val junit_bom = "org.junit:junit-bom:${Versions.junit}"
    const val junit_jupiter = "org.junit.jupiter:junit-jupiter"
    const val assertj = "org.assertj:assertj-core:${Versions.assertj}"
    const val mockito = "org.mockito:mockito-core:${Versions.mockito}"
    const val mockito_kotlin = "org.mockito.kotlin:mockito-kotlin:${Versions.mockito_kotlin}"
    const val flr_core = "com.felipebz.flr:flr-core:${Versions.flr}"
    const val flr_xpath = "com.felipebz.flr:flr-xpath:${Versions.flr}"
    const val flr_toolkit = "com.felipebz.flr:flr-toolkit:${Versions.flr}"
    const val flr_testing_harness = "com.felipebz.flr:flr-testing-harness:${Versions.flr}"
    const val gson = "com.google.code.gson:gson:${Versions.gson}"
}
