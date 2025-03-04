-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/sql-injection.html
DECLARE
  record_value VARCHAR2(4000);
BEGIN
  get_record_2('Anybody '' OR service_type=''Merger''--',
               'Anything',
               record_value);
END;
/