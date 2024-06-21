-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-subprograms.html
BEGIN
  DBMS_RESULT_CACHE.Bypass(FALSE);
END;
/