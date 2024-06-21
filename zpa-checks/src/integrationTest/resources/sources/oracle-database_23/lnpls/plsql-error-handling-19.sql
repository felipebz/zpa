-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-error-handling.html
BEGIN
  select_item('emp', 'last_name');
END;
/