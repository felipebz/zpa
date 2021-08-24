CREATE TABLE parallel_table (col1 number, col2 VARCHAR2(10)) PARALLEL 5; 

SELECT /*+ PARALLEL (MANUAL) */ col2
  FROM parallel_table;