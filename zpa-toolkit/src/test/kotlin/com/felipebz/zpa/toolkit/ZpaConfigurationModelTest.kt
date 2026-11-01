//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.toolkit

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

class ZpaConfigurationModelTest {

    @Test
    fun getConfiguration_charset() {
        val model = ZpaConfigurationModel()
        model.charsetProperty.value = "UTF-8"
        assertThat(model.charset).isEqualTo(StandardCharsets.UTF_8)
        assertThat(model.configuration.charset).isEqualTo(StandardCharsets.UTF_8)
        model.charsetProperty.value = "ISO-8859-1"
        assertThat(model.charset).isEqualTo(StandardCharsets.ISO_8859_1)
        assertThat(model.configuration.charset).isEqualTo(StandardCharsets.ISO_8859_1)
    }

    @Test
    fun getPropertyOrDefaultValue_with_property_set() {
        val oldValue = System.getProperty("foo")

        try {
            System.setProperty("foo", "bar")
            assertThat(ZpaConfigurationModel.getPropertyOrDefaultValue("foo", "baz")).isEqualTo("bar")
        } finally {
            if (oldValue == null) {
                System.clearProperty("foo")
            } else {
                System.setProperty("foo", oldValue)
            }
        }
    }

    @Test
    fun getPropertyOrDefaultValue_with_property_not_set() {
        val oldValue = System.getProperty("foo")

        try {
            System.clearProperty("foo")
            assertThat(ZpaConfigurationModel.getPropertyOrDefaultValue("foo", "baz")).isEqualTo("baz")
        } finally {
            if (oldValue == null) {
                System.clearProperty("foo")
            } else {
                System.setProperty("foo", oldValue)
            }
        }
    }

    @Test
    fun invalidProjectSourcesPropertyFallsBackToDefault() {
        val oldValue = System.getProperty("sonar.zpa.toolkit.projectSources")

        try {
            System.setProperty("sonar.zpa.toolkit.projectSources", "/no/such/path")
            assertThat(ZpaConfigurationModel().projectSourcesProperty.value).isEmpty()
        } finally {
            if (oldValue == null) {
                System.clearProperty("sonar.zpa.toolkit.projectSources")
            } else {
                System.setProperty("sonar.zpa.toolkit.projectSources", oldValue)
            }
        }
    }

    @Test
    fun propertiesAreListedInConfigurationTabOrder() {
        assertThat(ZpaConfigurationModel().properties.map { it.name })
            .containsExactly("Charset", "Error recovery", "Conditional compilation flags", "Database version", "Project sources", "Project file suffixes")
    }
}
