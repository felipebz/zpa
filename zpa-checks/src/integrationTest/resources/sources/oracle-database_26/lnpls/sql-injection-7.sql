-- https://docs.oracle.com/en/database/oracle/oracle-database/26/lnpls/sql-injection.html
SET SERVEROUTPUT ON;
BEGIN
  p('Andy', 'Waiter');
END;
/