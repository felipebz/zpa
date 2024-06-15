-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ALTER-INDEX.html
/* This example will fail if the tablespace in which partition p3
   resides is locally managed.
*/
ALTER INDEX cost_ix MODIFY PARTITION p3
   STORAGE(MAXEXTENTS 30) LOGGING;