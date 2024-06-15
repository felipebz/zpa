-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-TABLESPACE.html
CREATE BIGFILE TABLESPACE sh_lwp1 DATAFILE sh_lwp1.df SIZE 10M BLOCKSIZE 8K 
  LOST WRITE PROTECTION;