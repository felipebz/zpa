-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/subprogram-parameters.html
CREATE OR REPLACE PROCEDURE p (x OUT INTEGER, y OUT INTEGER) AS
BEGIN
  x := 17; y := 93;
END;
/