-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/ALTER-INDEX.html
ALTER INDEX cost_ix
   SPLIT PARTITION p2 AT (1500) 
   INTO ( PARTITION p2a TABLESPACE tbs_01 LOGGING,
          PARTITION p2b TABLESPACE tbs_02);