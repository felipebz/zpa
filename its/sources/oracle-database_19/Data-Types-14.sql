-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Data-Types.html
CREATE TABLE time_table
  (start_time    TIMESTAMP,
   duration_1    INTERVAL DAY (6) TO SECOND (5),
   duration_2    INTERVAL YEAR TO MONTH);