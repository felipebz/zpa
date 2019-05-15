/*
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
package org.sonar.plsqlopen.symbols;

import com.sonar.sslr.api.AstNode;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.symbol.NewSymbol;
import org.sonar.api.batch.sensor.symbol.NewSymbolTable;
import org.sonar.plsqlopen.TokenLocation;
import org.sonar.plugins.plsqlopen.api.symbols.Symbol;

import java.util.List;

public class SonarQubeSymbolTable {

    private final NewSymbolTable symbolizable;

    public SonarQubeSymbolTable(SensorContext context, InputFile inputFile) {
        symbolizable = context.newSymbolTable().onFile(inputFile);
    }

    public void save(List<Symbol> symbols) {
        for (Symbol symbol : symbols) {
            AstNode symbolNode = symbol.declaration();

            TokenLocation symbolLocation = TokenLocation.from(symbolNode.getToken());
            NewSymbol newSymbol = symbolizable.newSymbol(symbolLocation.line(), symbolLocation.column(),
                symbolLocation.endLine(), symbolLocation.endColumn());

            for (AstNode usage : symbol.usages()) {
                TokenLocation usageLocation = TokenLocation.from(usage.getToken());
                newSymbol.newReference(usageLocation.line(), usageLocation.column(), usageLocation.endLine(), usageLocation.endColumn());
            }
        }
        symbolizable.save();
    }
}
