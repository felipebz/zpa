-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/REGEXP_REPLACE.html
WITH strings AS (   
  SELECT 'abc123' s from DUAL union all   
  SELECT '123abc' s from DUAL union all   
  SELECT 'a1b2c3' s from DUAL   
)   
  SELECT s "STRING", REGEXP_REPLACE(s, '[0-9]', '', 1, 2) "MODIFIED_STRING"  
  FROM   strings;