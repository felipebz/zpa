//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

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
