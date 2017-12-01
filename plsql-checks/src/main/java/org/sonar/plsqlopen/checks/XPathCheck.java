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

import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.plsqlopen.annnotations.RuleTemplate;

import com.google.common.base.Strings;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.xpath.api.AstNodeXPathQuery;

@Rule(key = XPathCheck.CHECK_KEY, priority = Priority.MAJOR)
@RuleTemplate
public class XPathCheck extends AbstractBaseCheck {

    public static final String CHECK_KEY = "XPath";

    private static final String DEFAULT_XPATH_QUERY = "";
    private static final String DEFAULT_MESSAGE = "The XPath expression matches this piece of code";

    @RuleProperty(key = "xpathQuery", defaultValue = "" + DEFAULT_XPATH_QUERY)
    public String xpathQuery = DEFAULT_XPATH_QUERY;

    @RuleProperty(key = "message", defaultValue = "" + DEFAULT_MESSAGE)
    public String message = DEFAULT_MESSAGE;

    private AstNodeXPathQuery<Object> query = null;

    public AstNodeXPathQuery<Object> query() {
        if (query == null && !Strings.isNullOrEmpty(xpathQuery)) {
            try {
                query = AstNodeXPathQuery.create(xpathQuery);
            } catch (RuntimeException e) {
                throw new IllegalStateException(
                        "Unable to initialize the XPath engine, perhaps because of an invalid query: " + xpathQuery, e);
            }
        }
        return query;
    }

    @Override
    public void visitFile(AstNode fileNode) {
        if (query() != null) {
            List<Object> objects = query().selectNodes(fileNode);

            for (Object object : objects) {
                if (object instanceof AstNode) {
                    AstNode astNode = (AstNode) object;
                    getContext().createViolation(this, message, astNode);
                } else if (object instanceof Boolean && (Boolean) object) {
                    getContext().createFileViolation(this, message);
                }
            }
        }
    }

}
