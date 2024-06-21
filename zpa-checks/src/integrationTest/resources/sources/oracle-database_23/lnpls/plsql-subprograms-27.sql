-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-subprograms.html
CREATE OR REPLACE PROCEDURE p (
  n NUMBER
) AUTHID DEFINER IS
BEGIN
  NULL;
END;
/
DECLARE
  x NUMBER      :=  1;
  y VARCHAR2(1) := '1';
BEGIN
  p(x);             -- No conversion needed
  p(y);             -- z implicitly converted from VARCHAR2 to NUMBER
  p(TO_NUMBER(y));  -- z explicitly converted from VARCHAR2 to NUMBER
END;
/