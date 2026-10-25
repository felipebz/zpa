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
@file:OptIn(ZpaExperimentalApi::class)

package com.felipebz.zpa.symbols

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.semantics.HighlightingPair
import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.sensor.SensorContext
import org.sonar.api.batch.sensor.symbol.NewSymbolTable

class SonarQubeSymbolTable(context: SensorContext, inputFile: InputFile) {

    private val symbolizable: NewSymbolTable = context.newSymbolTable().onFile(inputFile)

    fun save(pairs: List<HighlightingPair>) {
        for (pair in pairs) {
            val declaration = pair.declaration
            val symbol = symbolizable.newSymbol(declaration.startLine, declaration.startColumn, declaration.endLine, declaration.endColumn)
            for (reference in pair.references) {
                symbol.newReference(reference.startLine, reference.startColumn, reference.endLine, reference.endColumn)
            }
        }
        symbolizable.save()
    }

}
