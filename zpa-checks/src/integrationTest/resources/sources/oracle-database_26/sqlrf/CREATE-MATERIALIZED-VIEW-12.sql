-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-MATERIALIZED-VIEW.html
CREATE MATERIALIZED VIEW MView1(T ANNOTATIONS (Hidden)) ANNOTATIONS (Title 'Tab1 MV1', ADD Snapshot) 
   AS SELECT * from Table1;