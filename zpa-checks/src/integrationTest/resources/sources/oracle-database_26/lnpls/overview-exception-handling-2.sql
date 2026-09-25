-- https://docs.oracle.com/en/database/oracle/oracle-database/26/lnpls/overview-exception-handling.html
BEGIN
  select_item('departments', 'last_name');
END;
/