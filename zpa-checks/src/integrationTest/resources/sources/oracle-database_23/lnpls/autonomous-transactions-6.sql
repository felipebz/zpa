-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/autonomous-transactions.html
DROP TABLE temp;
CREATE TABLE temp (
  temp_id NUMBER(6),
  up_date DATE
);
CREATE OR REPLACE TRIGGER drop_temp_table
  AFTER INSERT ON log
DECLARE 
  PRAGMA AUTONOMOUS_TRANSACTION;
BEGIN
  EXECUTE IMMEDIATE 'DROP TABLE temp';
  COMMIT;
END;
/
SELECT * FROM temp;