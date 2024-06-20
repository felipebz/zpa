-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ALTER-TABLESPACE.html
ALTER TABLESPACE temp_demo ADD TEMPFILE 'temp05.dbf' SIZE 5 AUTOEXTEND ON;
ALTER TABLESPACE temp_demo DROP TEMPFILE 'temp05.dbf';