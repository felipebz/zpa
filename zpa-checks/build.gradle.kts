dependencies {
    implementation(Libs.flr_core)
    implementation(Libs.flr_xpath)
    implementation(project(":zpa-core"))
    testImplementation(project(":plsql-checks-testkit"))
}

description = "Z PL/SQL Analyzer :: Checks"
