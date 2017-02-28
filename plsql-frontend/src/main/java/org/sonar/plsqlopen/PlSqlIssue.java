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
package org.sonar.plsqlopen;

import javax.annotation.Nullable;

import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;

public class PlSqlIssue {
    private final NewIssue newIssue;

    public PlSqlIssue(NewIssue newIssue) {
      this.newIssue = newIssue;
    }

    public static PlSqlIssue create(SensorContext context, RuleKey ruleKey, @Nullable Double gap) {
        NewIssue newIssue = context.newIssue().forRule(ruleKey).gap(gap);
        return new PlSqlIssue(newIssue);
    }

    public PlSqlIssue setPrimaryLocationOnFile(InputPath file, String message) {
        newIssue.at(newIssue.newLocation().on(file).message(message));
        return this;
    }

    public PlSqlIssue setPrimaryLocation(InputFile file, String message, int startLine, int startLineOffset, int endLine,
            int endLineOffset) {
        NewIssueLocation newIssueLocation;
        if (startLineOffset == -1) {
            newIssueLocation = newIssue.newLocation().on(file).at(file.selectLine(startLine)).message(message);
        } else {
            newIssueLocation = newIssue.newLocation().on(file)
                    .at(file.newRange(startLine, startLineOffset, endLine, endLineOffset)).message(message);
        }
        newIssue.at(newIssueLocation);
        return this;
    }

    public PlSqlIssue addSecondaryLocation(InputFile file, int startLine, int startLineOffset, int endLine,
            int endLineOffset, String message) {
        newIssue.addLocation(newIssue.newLocation().on(file)
                .at(file.newRange(startLine, startLineOffset, endLine, endLineOffset)).message(message));
        return this;
    }

    public void save() {
        newIssue.save();
    }
}
