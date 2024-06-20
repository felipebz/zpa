-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/REGEXP_REPLACE.html
WITH strings AS (   
  SELECT 'abc123' s FROM dual union all   
  SELECT '123abc' s FROM dual union all   
  SELECT 'a1b2c3' s FROM dual   
)   
  SELECT s "STRING", regexp_replace(s, '[0-9]', '') "MODIFIED_STRING"  
  FROM strings;