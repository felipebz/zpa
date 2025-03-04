-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/sql-injection.html
BEGIN
  p('Anybody', 'Anything'');
  DELETE FROM secret_records WHERE service_type=INITCAP(''Merger');
END;
/