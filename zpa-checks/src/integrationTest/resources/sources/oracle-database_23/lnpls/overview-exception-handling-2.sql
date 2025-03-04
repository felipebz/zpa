-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/overview-exception-handling.html
BEGIN
  select_item('departments', 'last_name');
END;
/