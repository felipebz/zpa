-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Comments.html
INSERT /*+ APPEND PARALLEL(target_table, 16) PQ_DISTRIBUTE(target_table, NONE) */
  INTO target_table
  SELECT * FROM source_table;