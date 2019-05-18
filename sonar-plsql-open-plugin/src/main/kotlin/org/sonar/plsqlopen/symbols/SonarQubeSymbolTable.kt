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
package org.sonar.plsqlopen.symbols

import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.sensor.SensorContext
import org.sonar.api.batch.sensor.symbol.NewSymbolTable
import org.sonar.plsqlopen.TokenLocation
import org.sonar.plugins.plsqlopen.api.symbols.Symbol

class SonarQubeSymbolTable(context: SensorContext, inputFile: InputFile) {

    private val symbolizable: NewSymbolTable = context.newSymbolTable().onFile(inputFile)

    fun save(symbols: List<Symbol>) {
        for (symbol in symbols) {
            val symbolNode = symbol.declaration()

            val symbolLocation = TokenLocation.from(symbolNode.token)
            val newSymbol = symbolizable.newSymbol(symbolLocation.line(), symbolLocation.column(),
                    symbolLocation.endLine(), symbolLocation.endColumn())

            for (usage in symbol.usages()) {
                val usageLocation = TokenLocation.from(usage.token)
                newSymbol.newReference(usageLocation.line(), usageLocation.column(), usageLocation.endLine(), usageLocation.endColumn())
            }
        }
        symbolizable.save()
    }

}
