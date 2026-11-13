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
            assertThat(metadata.blocks[0].itemDefinitions.map { it.dataType }).containsOnlyNulls()
            assertThat(metadata.blocks[1].name).isEqualTo("bar")
            assertThat(metadata.blocks[1].items).containsExactly("item1", "item2")
            assertThat(metadata.parameters).isEmpty()
            assertThat(metadata.lovs).containsExactly("foo", "bar")
        }
    }

    @Test
    fun readsItemsWrittenAsNamesOrObjectsAndParameters() {
        val metadata = FormsMetadata.loadFromFile("src/test/resources/metadata/typed_metadata.json")!!
        val orders = metadata.blocks[0]
        assertThat(orders.items).containsExactly("note", "order_id", "customer", "placed_on", "label", "legacy_qty")
        assertThat(orders.itemDefinitions.map { it.toString() })
            .containsExactly("note", "order_id NUMBER(10)", "customer", "placed_on DATE", "label CHAR(40)", "legacy_qty RNUMBER")
        assertThat(metadata.blocks[1].items).containsExactly("line_no", "amount")
        assertThat(metadata.parameters.map { it.toString() }).containsExactly("p_mode CHAR(10)", "p_order NUMBER", "p_any")
        assertThat(metadata.alerts).containsExactly("confirm_exit")
        assertThat(metadata.lovs).containsExactly("lov_customers")
    }

    @Test
    fun theTypeAReferenceHas() {
        val metadata = FormsMetadata.loadFromFile("src/test/resources/metadata/typed_metadata.json")!!
        fun type(qualifier: String, name: String) = FormsHostVariables.type(metadata, qualifier, name)
        // The block and item names are not case-sensitive.
        assertThat(type("ORDERS", "Order_Id")).isEqualTo("NUMBER")
        assertThat(type("orders", "placed_on")).isEqualTo("DATE")
        assertThat(type("orders", "label")).isEqualTo("VARCHAR2")
        // No data type, a legacy one, an item or a block the metadata does not list: not known.
        assertThat(listOf(type("orders", "note"), type("orders", "customer"), type("orders", "legacy_qty"), type("orders", "status"), type("other", "order_id")))
            .containsOnlyNulls()
        assertThat(type("parameter", "p_order")).isEqualTo("NUMBER")
        assertThat(type("PARAMETER", "p_mode")).isEqualTo("VARCHAR2")
        assertThat(type("parameter", "p_any")).isNull()
        assertThat(type("global", "user_id")).isEqualTo("VARCHAR2")
        assertThat(type("system", "cursor_record")).isEqualTo("VARCHAR2")
        assertThat(type("system", "tab_new_page")).isNull()
        assertThat(type("system", "trigger_node")).isNull()
        assertThat(listOf("CHAR", "LONG", "NCHAR", "NUMBER", "DATE", "DATETIME", "ALPHA", "INT", "MONEY", "RINT", "RMONEY", "RNUMBER", "EDATE", "JDATE", "TIME", "REF").map(FormsHostVariables::of))
            .containsExactly("VARCHAR2", "VARCHAR2", "NVARCHAR2", "NUMBER", "DATE", "DATE", null, null, null, null, null, null, null, null, null, null)
    }


    @Test
    fun returnsNullIfFileDoesNotExists() {
        val metadata = FormsMetadata.loadFromFile("src/test/resources/metadata/metadata2.json")

        assertThat(metadata).isNull()
    }

}
