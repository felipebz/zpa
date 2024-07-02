object Versions {
    const val junit = "5.10.2"
    const val assertj = "3.25.3"
    const val mockito = "5.12.0"
    const val mockito_kotlin = "5.3.1"
    const val flr = "1.4.0"
    const val jackson = "2.17.1"
    const val jsoup = "1.17.2"
    const val min_sonarqube = "9.9.0.65466"
    const val min_sonarqube_api = "9.14.0.375"
    const val sonarqube_orchestrator = "5.0.0.2065"
    const val woodstox = "6.6.2"

    const val plugin_shadow = "8.1.7"
}

object Libs {
    const val assertj = "org.assertj:assertj-core:${Versions.assertj}"
    const val mockito = "org.mockito:mockito-core:${Versions.mockito}"
    const val mockito_kotlin = "org.mockito.kotlin:mockito-kotlin:${Versions.mockito_kotlin}"
    const val flr_core = "com.felipebz.flr:flr-core:${Versions.flr}"
    const val flr_xpath = "com.felipebz.flr:flr-xpath:${Versions.flr}"
    const val flr_toolkit = "com.felipebz.flr:flr-toolkit:${Versions.flr}"
    const val flr_testing_harness = "com.felipebz.flr:flr-testing-harness:${Versions.flr}"
    const val jackson = "com.fasterxml.jackson.core:jackson-databind:${Versions.jackson}"
    const val jackson_xml = "com.fasterxml.jackson.dataformat:jackson-dataformat-xml:${Versions.jackson}"
    const val woodstox = "com.fasterxml.woodstox:woodstox-core:${Versions.woodstox}"
}
