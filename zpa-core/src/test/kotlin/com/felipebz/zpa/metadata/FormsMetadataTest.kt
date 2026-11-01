//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.metadata

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
