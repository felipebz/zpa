-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-MATERIALIZED-VIEW-LOG.html
CREATE MATERIALIZED VIEW LOG ON sales
   PCTFREE 5 
   TABLESPACE example 
   STORAGE (INITIAL 10K)
   FOR SYNCHRONOUS REFRESH USING mystage_log;