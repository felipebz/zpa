-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/conditional-compilation1.html
CREATE OR REPLACE PACKAGE cc_pkg
AUTHID DEFINER
$IF $$XFLAG $THEN ACCESSIBLE BY(p1_pkg) $END
IS
    i NUMBER := 10;
    trace CONSTANT BOOLEAN := TRUE;
END cc_pkg;