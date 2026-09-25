-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-TABLESPACE.html
CREATE BIGFILE TABLESPACE bigtbs_01
  DATAFILE 'bigtbs_f1.dbf'
  SIZE 20M AUTOEXTEND ON;