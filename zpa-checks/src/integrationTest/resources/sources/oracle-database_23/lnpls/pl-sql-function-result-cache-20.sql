-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/pl-sql-function-result-cache.html
BEGIN
  DBMS_RESULT_CACHE.Bypass(FALSE);
END;
/