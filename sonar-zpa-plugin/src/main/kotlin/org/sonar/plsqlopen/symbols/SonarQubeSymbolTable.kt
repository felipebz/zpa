/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2025 Felipe Zorzo
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
package org.sonar.plsqlopen.symbols

import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.sensor.SensorContext
import org.sonar.api.batch.sensor.symbol.NewSymbolTable
import org.sonar.plugins.plsqlopen.api.symbols.Symbol

class SonarQubeSymbolTable(context: SensorContext, inputFile: InputFile) {

    private val symbolizable: NewSymbolTable = context.newSymbolTable().onFile(inputFile)

    fun save(symbols: List<Symbol>) {
        for (symbol in symbols) {
            val symbolNode = symbol.declaration

            val symbolToken = symbolNode.token
            val newSymbol = symbolizable.newSymbol(symbolToken.line, symbolToken.column,
                symbolToken.endLine, symbolToken.endColumn)

            for (usage in symbol.usages) {
                val usageToken = usage.token
                newSymbol.newReference(usageToken.line, usageToken.column, usageToken.endLine, usageToken.endColumn)
            }
        }
        symbolizable.save()
    }

}
