-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/sql-injection.html
SET SERVEROUTPUT ON;
DECLARE
  record_value VARCHAR2(4000);
BEGIN
  get_record('Andy', 'Waiter', record_value);
END;
/