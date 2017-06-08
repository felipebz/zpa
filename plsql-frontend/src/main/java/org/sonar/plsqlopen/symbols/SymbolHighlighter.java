package org.sonar.plsqlopen.symbols;

import org.sonar.api.batch.sensor.symbol.NewSymbol;
import org.sonar.api.batch.sensor.symbol.NewSymbolTable;
import org.sonar.plsqlopen.TokenLocation;
import org.sonar.plugins.plsqlopen.api.symbols.Symbol;
import org.sonar.plugins.plsqlopen.api.symbols.SymbolTable;

import com.sonar.sslr.api.AstNode;

public class SymbolHighlighter {
    
    public void highlight(NewSymbolTable symbolizable, SymbolTable symbolTable) {
        for (Symbol symbol : symbolTable.getSymbols()) {
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
