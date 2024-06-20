-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-ROLLBACK-SEGMENT.html
CREATE TABLESPACE rbs_ts
   DATAFILE 'rbs01.dbf' SIZE 10M
   EXTENT MANAGEMENT LOCAL UNIFORM SIZE 100K;
CREATE ROLLBACK SEGMENT rbs_one
   TABLESPACE rbs_ts;