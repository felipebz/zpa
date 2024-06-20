-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/REGEXP_REPLACE.html
WITH strings AS (   
  SELECT 'Hello  World' s FROM dual union all   
  SELECT 'Hello        World' s FROM dual union all   
  SELECT 'Hello,   World  !' s FROM dual   
)   
  SELECT s "STRING", regexp_replace(s, ' {2,}', ' ') "MODIFIED_STRING"  
  FROM   strings;