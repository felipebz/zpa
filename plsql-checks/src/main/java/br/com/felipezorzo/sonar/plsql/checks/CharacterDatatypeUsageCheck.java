/*
 * Sonar PL/SQL Plugin (Community)
 * Copyright (C) 2015 Felipe Zorzo
 * felipe.b.zorzo@gmail.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package br.com.felipezorzo.sonar.plsql.checks;

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import com.sonar.sslr.api.AstNode;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.PlSqlKeyword;

@Rule(
    key = CharacterDatatypeUsageCheck.CHECK_KEY,
    priority = Priority.MINOR,
    tags = Tags.OBSOLETE
)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.DATA_CHANGEABILITY)
@SqaleConstantRemediation("5min")
@ActivatedByDefault
public class CharacterDatatypeUsageCheck extends AbstractBaseCheck {

    public static final String CHECK_KEY = "CharacterDatatypeUsage";

    @Override
    public void init() {
        subscribeTo(PlSqlGrammar.CHARACTER_DATAYPE);
    }

    @Override
    public void visitNode(AstNode node) {
        AstNode datatype = node.getFirstChild();
        if (datatype.is(PlSqlKeyword.CHAR, PlSqlKeyword.VARCHAR)) {
            getContext().createLineViolation(this, getLocalizedMessage(CHECK_KEY), node, datatype.getTokenValue());
        }
    }

}
