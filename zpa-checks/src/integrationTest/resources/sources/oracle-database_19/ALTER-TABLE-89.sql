-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/ALTER-TABLE.html
ALTER TABLE employees ADD (resume CLOB)
LOB (resume) STORE AS SECUREFILE resume_seg (TABLESPACE auto_seg_ts);