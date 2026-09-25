-- https://docs.oracle.com/en/database/oracle/oracle-database/26/lnpls/pl-sql-function-result-cache.html
BEGIN
  DBMS_RESULT_CACHE.Bypass(TRUE);
  DBMS_RESULT_CACHE.Flush;
END;
/