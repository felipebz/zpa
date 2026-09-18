package com.felipebz.zpa

plugins {
    id("org.jreleaser")
}

jreleaser {
    project {
        description.set("ZPA - Parser and static code analysis tool for PL/SQL and Oracle SQL")
        authors.set(listOf("felipebz"))
        license.set("LGPL-3.0")
        links {
            homepage.set("https://zpa.felipebz.com")
        }
        inceptionYear.set("2015")
        snapshot {
            fullChangelog.set(true)
        }
    }
    release {
        github {
            overwrite.set(true)
            tagName.set("{{projectVersion}}")
            draft.set(true)
            changelog {
                formatted.set(org.jreleaser.model.Active.ALWAYS)
                preset.set("conventional-commits")
                contentTemplate.set(file("template/changelog.tpl"))
                extraProperties.put("categorizeScopes", "true")
                format.set(
                    "- {{#conventionalCommitIsBreakingChange}}🚨 {{/conventionalCommitIsBreakingChange}}" +
                        "{{conventionalCommitDescription}}" +
                        "{{#conventionalCommitBreakingChangeContent}} - *{{conventionalCommitBreakingChangeContent}}*" +
                        "{{/conventionalCommitBreakingChangeContent}}"
                )
                contributors {
                    enabled.set(true)
                }
                hide {
                    uncategorized.set(true)
                    categories.addAll(
                        listOf(
                            "merge",
                            "test",
                            "tasks",
                            "build",
                            "docs"
                        )
                    )
                }

                labeler {
                    label.set("internal")
                    title.set("regex:^(?:[a-z]+)(?:\\(deps\\))?!?:.*")
                }
                labeler {
                    label.set("internal")
                    title.set("regex:^(?:refactor|style)(?:\\([^)]*\\))?!?:.*")
                }

                excludeLabels.add("internal")
            }
        }
    }
    distributions {
        listOf("sonar-zpa-plugin", "zpa-toolkit").forEach {
            create(it) {
                artifact {
                    path.set(file("{{distributionName}}/build/libs/{{distributionName}}-{{projectVersion}}.jar"))
                }
            }
        }
        create("plsql-custom-rules") {
            artifact {
                path.set(file("build/{{distributionName}}.zip"))
            }
        }
    }
    signing {
        active.set(org.jreleaser.model.Active.ALWAYS)
        armored.set(true)
    }
    deploy {
        maven {
            mavenCentral {
                create("sonatype") {
                    active.set(org.jreleaser.model.Active.RELEASE)
                    snapshotSupported.set(true)
                    url.set("https://central.sonatype.com/api/v1/publisher")
                    stagingRepository("build/staging-deploy")
                }
            }
            nexus2 {
                create("snapshot-deploy") {
                    active.set(org.jreleaser.model.Active.SNAPSHOT)
                    url.set("https://central.sonatype.com/repository/maven-snapshots")
                    snapshotUrl.set("https://central.sonatype.com/repository/maven-snapshots")
                    applyMavenCentralRules.set(true)
                    snapshotSupported.set(true)
                    closeRepository.set(true)
                    releaseRepository.set(true)
                    stagingRepository("build/staging-deploy")
                }
            }
        }
    }
}
