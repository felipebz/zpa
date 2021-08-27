-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/CREATE-TABLESPACE.html
CREATE BIGFILE TABLESPACE <shadow_tablespace_name> 
  DATAFILE <datafile_name> 
  SIZE 100000000 BLOCKSIZE 8K 
  LOST WRITE PROTECTION