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
package org.sonar.plsqlopen;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Grammar;
import com.sonar.sslr.impl.Parser;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.plugins.plsqlopen.api.PlSqlFile;
import org.sonar.plugins.plsqlopen.api.PlSqlVisitorContext;
import org.sonar.plugins.plsqlopen.api.checks.PlSqlVisitor;
import org.sonar.plsqlopen.metadata.FormsMetadata;
import org.sonar.plsqlopen.parser.PlSqlParser;
import org.sonar.plsqlopen.squid.PlSqlAstScanner;
import org.sonar.plsqlopen.squid.PlSqlConfiguration;

public class TestPlSqlVisitorRunner {

  private TestPlSqlVisitorRunner() {
  }

  public static void scanFile(File file, FormsMetadata metadata, PlSqlVisitor... visitors) {
    PlSqlVisitorContext context = createContext(file, metadata);
    for (PlSqlVisitor visitor : visitors) {
      visitor.scanFile(context);
    }
  }

  public static PlSqlVisitorContext createContext(File file, FormsMetadata metadata) {
    Parser<Grammar> parser = PlSqlParser.create(new PlSqlConfiguration(StandardCharsets.UTF_8));
    TestPlSqlFile plSqlFile = new TestPlSqlFile(file);
    AstNode rootTree = PlSqlAstScanner.getSemanticNode(parser.parse(plSqlFile.contents()));
    return new PlSqlVisitorContext(rootTree, plSqlFile, metadata);
  }

  private static class TestPlSqlFile implements PlSqlFile {

    private final File file;

    public TestPlSqlFile(File file) {
      this.file = file;
    }

    @Override
    public String contents() {
      try {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
      } catch (IOException e) {
        throw new IllegalStateException("Cannot read " + file, e);
      }
    }

    @Override
    public String fileName() {
      return file.getName();
    }

  }

}
