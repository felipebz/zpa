-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/EXPLAIN-PLAN.html
SELECT operation, options, partition_start, partition_stop,
       partition_id
  FROM plan_table;