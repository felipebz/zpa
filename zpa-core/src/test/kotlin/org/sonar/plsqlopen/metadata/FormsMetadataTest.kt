/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2021 Felipe Zorzo
 * mailto:felipe AT felipezorzo DOT com DOT br
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plsqlopen.metadata

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail

import org.junit.jupiter.api.Test

class FormsMetadataTest {

    @Test
    fun canReadSimpleMetadaFile() {
        val metadata = FormsMetadata.loadFromFile("src/test/resources/metadata/metadata.json")

        if (metadata == null) {
            fail("Should load Oracle Forms metadata")
        } else {
            assertThat(metadata.alerts).containsExactly("foo", "bar")
            assertThat(metadata.blocks).hasSize(2)
            assertThat(metadata.blocks[0].name).isEqualTo("foo")
            assertThat(metadata.blocks[0].items).containsExactly("item1", "item2")
            assertThat(metadata.blocks[1].name).isEqualTo("bar")
            assertThat(metadata.blocks[1].items).containsExactly("item1", "item2")
            assertThat(metadata.lovs).containsExactly("foo", "bar")
        }
    }


    @Test
    fun returnsNullIfFileDoesNotExists() {
        val metadata = FormsMetadata.loadFromFile("src/test/resources/metadata/metadata2.json")

        assertThat(metadata).isNull()
    }

}
