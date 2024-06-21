-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-name-resolution.html
UPDATE hr.tb1 t SET t.col1.x = 10 WHERE t.col1.x = 20;
DECLARE
  y NUMBER;
BEGIN
  SELECT t.col1.x INTO y FROM tb1 t WHERE t.col1.x = 30;
END;
/
DELETE FROM tb1 t WHERE t.col1.x = 10;