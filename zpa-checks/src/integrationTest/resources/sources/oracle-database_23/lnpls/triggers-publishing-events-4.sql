-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/triggers-publishing-events.html
DECLARE
  v_db_name VARCHAR2(50);
BEGIN
  v_db_name := ora_database_name;
END;
/