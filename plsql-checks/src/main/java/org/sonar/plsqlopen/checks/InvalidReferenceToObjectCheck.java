/*
 * Sonar PL/SQL Plugin (Community)
 * Copyright (C) 2015-2016 Felipe Zorzo
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
package org.sonar.plsqlopen.checks;

import java.util.List;
import java.util.stream.Stream;

import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plsqlopen.FormsMetadataAwareCheck;
import org.sonar.plsqlopen.matchers.MethodMatcher;
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;

import com.google.common.collect.ImmutableList;
import com.sonar.sslr.api.AstNode;

@Rule(
    key = InvalidReferenceToObjectCheck.CHECK_KEY,
    priority = Priority.MAJOR
)
@SqaleConstantRemediation("5min")
@ActivatedByDefault
public class InvalidReferenceToObjectCheck extends AbstractBaseCheck implements FormsMetadataAwareCheck {

    public static final String CHECK_KEY = "InvalidReferenceToObject";
    
    private final List<Verifier> verifiers = ImmutableList.of(
            new Verifier(MethodMatcher.create().name("show_lov").addParameter(), 0, ObjectType.LOV)
        );

    @Override
    public void init() {
        subscribeTo(PlSqlGrammar.METHOD_CALL);
    }

    @Override
    public void visitNode(AstNode node) {
        for (Verifier verifier : verifiers) {
            if (verifier.matcher.matches(node)) {
                AstNode argument = verifier.matcher.getArguments(node).get(verifier.argumentToCheck);
                String value = argument.getTokenOriginalValue().replace("'", "");
                
                boolean reportIssue = false;
                if (verifier.type == ObjectType.LOV) {
                    reportIssue = !Stream.of(getPlSqlContext().getFormsMetadata().getLovs()).anyMatch(lov -> lov.equalsIgnoreCase(value));
                }
                
                if (reportIssue) {
                    getPlSqlContext().createViolation(this, getLocalizedMessage(CHECK_KEY), argument, value, verifier.matcher.getMethodName().toUpperCase());
                }
                
            }
        }
    }
    
    private enum ObjectType { LOV }
    
    private class Verifier {
        public final MethodMatcher matcher;
        public final int argumentToCheck;
        public final ObjectType type;

        public Verifier(MethodMatcher matcher, int argumentToCheck, ObjectType type) {
            this.matcher = matcher;
            this.argumentToCheck = argumentToCheck;
            this.type = type;
        }
    }

}
