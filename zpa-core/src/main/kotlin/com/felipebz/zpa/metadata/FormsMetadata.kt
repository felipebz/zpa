/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2026 Felipe Zorzo
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
