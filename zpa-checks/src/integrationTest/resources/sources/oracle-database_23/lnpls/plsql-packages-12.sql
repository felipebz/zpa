-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-packages.html
CREATE OR REPLACE PACKAGE pkg IS
  n NUMBER := 5;
END pkg;
/
CREATE OR REPLACE PACKAGE sr_pkg IS
  PRAGMA SERIALLY_REUSABLE;
  n NUMBER := 5;
END sr_pkg;
/
BEGIN
  pkg.n := 10;
  sr_pkg.n := 10;
END;
/
BEGIN
  DBMS_OUTPUT.PUT_LINE('pkg.n: ' || pkg.n);
  DBMS_OUTPUT.PUT_LINE('sr_pkg.n: ' || sr_pkg.n);
END;
/