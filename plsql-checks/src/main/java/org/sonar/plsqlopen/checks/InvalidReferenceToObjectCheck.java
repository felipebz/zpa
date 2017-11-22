/*
 * Sonar PL/SQL Plugin (Community)
 * Copyright (C) 2015-2017 Felipe Zorzo
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
import org.sonar.plsqlopen.metadata.Block;
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;
import org.sonar.plsqlopen.annnotations.ActivatedByDefault;
import org.sonar.plsqlopen.annnotations.ConstantRemediation;

import com.google.common.collect.ImmutableList;
import com.sonar.sslr.api.AstNode;

@Rule(
    key = InvalidReferenceToObjectCheck.CHECK_KEY,
    priority = Priority.MAJOR
)
@ConstantRemediation("5min")
@ActivatedByDefault
public class InvalidReferenceToObjectCheck extends AbstractBaseCheck implements FormsMetadataAwareCheck {

    public static final String CHECK_KEY = "InvalidReferenceToObject";
    
    private final List<Verifier> verifiers = ImmutableList.of(
            new Verifier(MethodMatcher.create().name("find_alert").addParameter(), ObjectType.ALERT),
            new Verifier(MethodMatcher.create().name("set_alert_button_property").addParameters(4), ObjectType.ALERT),
            new Verifier(MethodMatcher.create().name("set_alert_property").addParameters(3), ObjectType.ALERT),
            new Verifier(MethodMatcher.create().name("show_alert").addParameter(), ObjectType.ALERT),
            
            new Verifier(MethodMatcher.create().name("find_lov").addParameter(), ObjectType.LOV),
            new Verifier(MethodMatcher.create().name("get_lov_property").addParameters(2), ObjectType.LOV),
            new Verifier(MethodMatcher.create().name("set_lov_column_property").addParameters(4), ObjectType.LOV),
            new Verifier(MethodMatcher.create().name("set_lov_property").addParameters(3), ObjectType.LOV),
            new Verifier(MethodMatcher.create().name("set_lov_property").addParameters(4), ObjectType.LOV),
            new Verifier(MethodMatcher.create().name("show_lov").addParameter(), ObjectType.LOV),
            
            new Verifier(MethodMatcher.create().name("find_block").addParameter(), ObjectType.BLOCK),
            new Verifier(MethodMatcher.create().name("get_block_property").addParameters(2), ObjectType.BLOCK),
            new Verifier(MethodMatcher.create().name("go_block").addParameter(), ObjectType.BLOCK),
            new Verifier(MethodMatcher.create().name("set_block_property").addParameters(3), ObjectType.BLOCK),
            new Verifier(MethodMatcher.create().name("set_block_property").addParameters(4), ObjectType.BLOCK),
            
            new Verifier(MethodMatcher.create().name("checkbox_checked").addParameter(), ObjectType.ITEM),
            new Verifier(MethodMatcher.create().name("convert_other_value").addParameter(), ObjectType.ITEM),
            new Verifier(MethodMatcher.create().name("display_item").addParameters(2), ObjectType.ITEM),
            new Verifier(MethodMatcher.create().name("find_item").addParameter(), ObjectType.ITEM),
            new Verifier(MethodMatcher.create().name("get_item_instance_property").addParameters(3), ObjectType.ITEM),
            new Verifier(MethodMatcher.create().name("get_item_property").addParameters(2), ObjectType.ITEM),
            new Verifier(MethodMatcher.create().name("get_radio_button_property").addParameters(3), ObjectType.ITEM),
            new Verifier(MethodMatcher.create().name("go_item").addParameter(), ObjectType.ITEM),
            new Verifier(MethodMatcher.create().name("image_scroll").addParameters(3), ObjectType.ITEM),
            new Verifier(MethodMatcher.create().name("image_zoom").addParameters(2), ObjectType.ITEM),
            new Verifier(MethodMatcher.create().name("image_zoom").addParameters(3), ObjectType.ITEM),
            new Verifier(MethodMatcher.create().name("play_sound").addParameter(), ObjectType.ITEM),
            new Verifier(MethodMatcher.create().name("read_image_file").addParameters(3), 3, ObjectType.ITEM),
            new Verifier(MethodMatcher.create().name("read_sound_file").addParameters(3), 3, ObjectType.ITEM),
            new Verifier(MethodMatcher.create().name("recalculate").addParameter(), ObjectType.ITEM),
            new Verifier(MethodMatcher.create().name("set_item_instance_property").addParameters(4), ObjectType.ITEM),
            new Verifier(MethodMatcher.create().name("set_item_property").addParameters(3), ObjectType.ITEM),
            new Verifier(MethodMatcher.create().name("set_item_property").addParameters(4), ObjectType.ITEM),
            new Verifier(MethodMatcher.create().name("set_radio_button_property").addParameters(4), ObjectType.ITEM),
            new Verifier(MethodMatcher.create().name("set_radio_button_property").addParameters(5), ObjectType.ITEM),
            new Verifier(MethodMatcher.create().name("write_image_file").addParameters(5), 3, ObjectType.ITEM),
            new Verifier(MethodMatcher.create().name("write_sound_file").addParameters(5), 3, ObjectType.ITEM)
        );

    @Override
    public void init() {
        subscribeTo(PlSqlGrammar.METHOD_CALL);
    }

    @Override
    public void visitNode(AstNode node) {
        Verifier verifier = verifiers.stream().filter(v -> v.matcher.matches(node)).findFirst().orElse(null);
        
        if (verifier != null) {
            AstNode argument = verifier.matcher.getArgumentsValues(node).get(verifier.argumentToCheck);
            if (!isVarcharLiteral(argument)) {
                return;
            }
            
            String value = argument.getTokenOriginalValue().replace("'", "");
            
            boolean reportIssue = false;
            if (verifier.type == ObjectType.ALERT) {
                reportIssue = validateAlert(value);
            } else if (verifier.type == ObjectType.BLOCK) {
                reportIssue = validateBlock(value);
            } else if (verifier.type == ObjectType.ITEM) {
                reportIssue = validateItem(value);
            } else if (verifier.type == ObjectType.LOV) {
                reportIssue = validateLov(value);
            }
            
            if (reportIssue) {
                getContext().createViolation(this, getLocalizedMessage(CHECK_KEY), argument, value, verifier.matcher.getMethodName().toUpperCase());
            }
            
        }
    }

    private boolean validateAlert(String value) {
        return !Stream.of(getContext().getFormsMetadata().getAlerts()).anyMatch(alert -> alert.equalsIgnoreCase(value));
    }
    
    private boolean validateBlock(String value) {
        return !Stream.of(getContext().getFormsMetadata().getBlocks()).anyMatch(block -> block.getName().equalsIgnoreCase(value));
    }
    
    private boolean validateItem(String value) {
        boolean reportIssue = true;
        for (Block block : getContext().getFormsMetadata().getBlocks()) {
            if (Stream.of(block.getItems()).anyMatch(item -> {
                    String fullName = block.getName() + "." + item;
                    return fullName.equalsIgnoreCase(value);
                })) {
                reportIssue = false;
            }
        }
        return reportIssue;
    }
    
    private boolean validateLov(String value) {
        return !Stream.of(getContext().getFormsMetadata().getLovs()).anyMatch(lov -> lov.equalsIgnoreCase(value));
    }
    
    private static boolean isVarcharLiteral(AstNode argument) {
        if (argument.is(PlSqlGrammar.LITERAL)) {
            return argument.hasDirectChildren(PlSqlGrammar.CHARACTER_LITERAL);
        }
        return false;
    }

    private enum ObjectType { ALERT, BLOCK, ITEM, LOV }
    
    private class Verifier {
        public final MethodMatcher matcher;
        public final int argumentToCheck;
        public final ObjectType type;

        public Verifier(MethodMatcher matcher, ObjectType type) {
            this(matcher, 1, type);
        }
        
        public Verifier(MethodMatcher matcher, int argumentToCheck, ObjectType type) {
            this.matcher = matcher;
            this.argumentToCheck = argumentToCheck - 1;
            this.type = type;
        }
    }

}
