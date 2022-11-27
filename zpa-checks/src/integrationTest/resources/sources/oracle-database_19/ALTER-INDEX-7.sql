-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/ALTER-INDEX.html
ALTER INDEX cost_ix 
   REBUILD PARTITION p2;
ALTER INDEX cost_ix
   REBUILD PARTITION p3 NOLOGGING;