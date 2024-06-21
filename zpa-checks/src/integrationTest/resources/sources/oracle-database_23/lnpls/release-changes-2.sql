-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/release-changes.html
CREATE PROCEDURE IF NOT EXISTS hello AS
BEGIN
  DBMS_OUTPUT.PUT_LINE('Hello there');
END;
/