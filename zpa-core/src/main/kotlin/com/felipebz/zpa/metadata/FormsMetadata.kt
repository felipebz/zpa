//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.metadata

import com.fasterxml.jackson.databind.ObjectMapper
import com.felipebz.zpa.utils.log.Loggers
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

class Block @JvmOverloads constructor(val name: String = "", val items: Array<String> = emptyArray())

class FormsMetadata {

    var alerts = listOf<String>()
    var blocks = listOf<Block>()
    var lovs = listOf<String>()

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
