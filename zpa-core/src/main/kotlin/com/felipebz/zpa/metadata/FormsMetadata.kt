//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.metadata

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.felipebz.zpa.utils.log.Loggers
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

/**
 * An item of a block: its [name], and, when the metadata states them, the [dataType] Forms Builder writes for it
 * (`CHAR`, `NUMBER`, `DATE`, `DATETIME`, `LONG`, or a legacy one such as `ALPHA` or `INT`) and its [maxLength]. In the
 * JSON an item is its name alone (`"order_id"`) or an object (`{"name": "order_id", "dataType": "NUMBER"}`). A
 * missing or unrecognized [dataType] means the type is not known.
 */
class FormsItem @JsonCreator(mode = JsonCreator.Mode.PROPERTIES) constructor(
    @JsonProperty("name") val name: String,
    @JsonProperty("dataType") val dataType: String? = null,
    @JsonProperty("maxLength") val maxLength: Int? = null
) {
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    constructor(name: String) : this(name, null, null)

    override fun toString() = name + (dataType?.let { " $it" } ?: "") + (maxLength?.let { "($it)" } ?: "")
}

/**
 * A parameter of the form (`:parameter.name`): its [name], and, when the metadata states them, its Forms [dataType]
 * and [maxLength], as for [FormsItem].
 */
class FormsParameter @JsonCreator(mode = JsonCreator.Mode.PROPERTIES) constructor(
    @JsonProperty("name") val name: String,
    @JsonProperty("dataType") val dataType: String? = null,
    @JsonProperty("maxLength") val maxLength: Int? = null
) {
    override fun toString() = name + (dataType?.let { " $it" } ?: "") + (maxLength?.let { "($it)" } ?: "")
}

/** A block of the form: its [name] and its items, in order. */
class Block @JsonCreator(mode = JsonCreator.Mode.PROPERTIES) constructor(
    @JsonProperty("name") val name: String,
    @JsonProperty("items") itemDefinitions: List<FormsItem>?
) {
    @JvmOverloads
    constructor(name: String = "", items: Array<String> = emptyArray()) : this(name, items.map(::FormsItem))

    /** The items, with what the metadata states of each. */
    val itemDefinitions: List<FormsItem> = itemDefinitions.orEmpty()

    /** The names of the items. */
    val items: Array<String> get() = itemDefinitions.map { it.name }.toTypedArray()
}

/**
 * What a scan knows of the Oracle Forms module its PL/SQL belongs to, read from JSON: the names of its alerts, its
 * blocks with their items, its lists of values and its parameters. Item and parameter data types type the host
 * variables that name them; nothing here decides whether a name belongs to the form, a library or the database.
 */
class FormsMetadata {

    var alerts = listOf<String>()
    var blocks = listOf<Block>()
    var lovs = listOf<String>()

    /** The parameters of the form; empty when the metadata lists none. */
    var parameters = listOf<FormsParameter>()

    companion object {
        private val LOG = Loggers.getLogger(FormsMetadata::class.java)

        fun loadFromFile(path: String?): FormsMetadata? {
            if (!path.isNullOrEmpty()) {
                try {
                    val mapper = ObjectMapper()
                    val formsMetadata: FormsMetadata = mapper.readValue(File(path), FormsMetadata::class.java)
                    LOG.info("Loaded Oracle Forms metadata from {}.", path)
                    return formsMetadata
                } catch (e: FileNotFoundException) {
                    LOG.warn("The metadata file {} was not found.", path, e)
                } catch (e: IOException) {
                    LOG.error("Error reading the metadata file at {}.", path, e)
                }

            }
            return null
        }
    }

}
