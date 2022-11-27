-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/ALTER-TABLESPACE.html
ALTER TABLESPACE tbs_03 
    ADD DATAFILE 'tbs_f04.dbf'
    SIZE 100K
    AUTOEXTEND ON
    NEXT 10K
    MAXSIZE 100K;