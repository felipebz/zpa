-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/subprogram-parts.html
CREATE OR REPLACE FUNCTION f (n INTEGER)
  RETURN INTEGER
  AUTHID DEFINER
IS
BEGIN
  IF n = 0 THEN
    RETURN 1;
  ELSIF n = 1 THEN
    RETURN n;
  END IF;
END;
/