-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/CREATE-ROLLBACK-SEGMENT.html
CREATE ROLLBACK SEGMENT rbs_one
   TABLESPACE rbs_ts
   STORAGE
   ( INITIAL 10K );