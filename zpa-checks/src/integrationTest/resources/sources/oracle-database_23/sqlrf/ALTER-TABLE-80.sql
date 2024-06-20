-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ALTER-TABLE.html
SELECT initial_extent, 
       next_extent, 
       min_extents, 
       max_extents, 
       pct_increase, 
       blocks, 
       sample_size 
FROM   user_tables 
WHERE  table_name = 'JOBS_TEMP';