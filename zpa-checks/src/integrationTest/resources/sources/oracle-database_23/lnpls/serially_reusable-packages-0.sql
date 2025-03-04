-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/serially_reusable-packages.html
CREATE OR REPLACE PACKAGE bodiless_pkg AUTHID DEFINER IS
  PRAGMA SERIALLY_REUSABLE;
  n NUMBER := 5;
END;
/
CREATE OR REPLACE PACKAGE pkg AUTHID DEFINER IS
  PRAGMA SERIALLY_REUSABLE;
  n NUMBER := 5;
END;
/
CREATE OR REPLACE PACKAGE BODY pkg IS
  PRAGMA SERIALLY_REUSABLE;
BEGIN
  n := 5;
END;
/