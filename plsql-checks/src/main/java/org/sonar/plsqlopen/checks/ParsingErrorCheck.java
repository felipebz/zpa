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
package org.sonar.plsqlopen.checks;

import java.io.StringWriter;

import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.squidbridge.AstScannerExceptionHandler;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.NoSqale;

import com.sonar.sslr.api.RecognitionException;

@Rule(
    key = ParsingErrorCheck.CHECK_KEY,
    priority = Priority.INFO
)
@NoSqale
@ActivatedByDefault
public class ParsingErrorCheck extends AbstractBaseCheck implements AstScannerExceptionHandler {

    public static final String CHECK_KEY = "ParsingError";

    @Override
    public void processException(Exception e) {
      StringWriter exception = new StringWriter();
      getContext().createFileViolation(this, exception.toString());
    }

    @Override
    public void processRecognitionException(RecognitionException e) {
      getContext().createLineViolation(this, e.getMessage(), e.getLine());
    }
    
}
