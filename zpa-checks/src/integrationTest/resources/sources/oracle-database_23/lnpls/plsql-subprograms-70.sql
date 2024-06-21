-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-subprograms.html
ALTER SESSION SET PLSQL_IMPLICIT_CONVERSION_BOOL = FALSE;
CREATE OR REPLACE PACKAGE pkg1 AUTHID DEFINER IS
  PROCEDURE s (p INTEGER);
  PROCEDURE s (p BOOLEAN);
END pkg1;
/
CREATE OR REPLACE PACKAGE BODY pkg1 IS
  PROCEDURE s (p INTEGER) AS
  BEGIN
    dbms_output.put_line ( 'Integer' );
  END;
  PROCEDURE s (p BOOLEAN) AS
  BEGIN
    dbms_output.put_line ( 'Boolean' );
  END;
END pkg1;
/
BEGIN
pkg1.s('1');  -- Compiles without error
END;
/