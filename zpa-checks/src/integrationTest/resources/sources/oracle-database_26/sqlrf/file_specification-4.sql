-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/file_specification.html
ALTER TABLESPACE stocks 
   ADD DATAFILE 'stock4.dbf' SIZE 10M REUSE;