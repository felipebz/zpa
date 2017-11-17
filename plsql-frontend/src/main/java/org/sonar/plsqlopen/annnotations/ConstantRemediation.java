package org.sonar.plsqlopen.annnotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ConstantRemediation {
    
    /**
     * Value of the remediation
     * @return e.g. "10min" or "2h"
     */
    String value();

}
