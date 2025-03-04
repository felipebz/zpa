-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/native-dynamic-sql.html
CREATE OR REPLACE PROCEDURE p (x BOOLEAN) AUTHID DEFINER AS
BEGIN
  IF x THEN
    DBMS_OUTPUT.PUT_LINE('x is true');
  END IF;
END;
/
DECLARE
  dyn_stmt VARCHAR2(200);
  b        BOOLEAN := TRUE;
BEGIN
  dyn_stmt := 'BEGIN p(:x); END;';
  EXECUTE IMMEDIATE dyn_stmt USING b;
END;
/