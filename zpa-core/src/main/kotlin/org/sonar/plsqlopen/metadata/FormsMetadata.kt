/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2019 Felipe Zorzo
 * mailto:felipebzorzo AT gmail DOT com
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

import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import org.sonar.plsqlopen.utils.log.Loggers
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.IOException

class Block(val name: String, val items: Array<String>)

class FormsMetadata {

    var alerts = mutableListOf<String>()
    var blocks = mutableListOf<Block>()
    var lovs = mutableListOf<String>()

    companion object {

        private val LOG = Loggers[FormsMetadata::class.java]

        @JvmStatic
        fun loadFromFile(path: String?): FormsMetadata? {
            if (!path.isNullOrEmpty()) {
                try {
                    JsonReader(FileReader(path)).use { reader ->
                        val formsMetadata = Gson().fromJson<FormsMetadata>(reader, FormsMetadata::class.java)
                        LOG.info("Loaded Oracle Forms metadata from {}.", path)
                        return formsMetadata
                    }
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
