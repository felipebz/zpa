-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/REGEXP_REPLACE.html
WITH strings as (   
  SELECT 'NEW YORK' s FROM dual union all   
  SELECT 'New York' s FROM dual union all   
  SELECT 'new york' s FROM dual   
)   
  SELECT s "STRING",  
        regexp_replace(s, '[a-z]', '1', 1, 0, 'i') "CASE_INSENSITIVE",  
        regexp_replace(s, '[a-z]', '1', 1, 0, 'c') "CASE_SENSITIVE",  
        regexp_replace(s, '[a-zA-Z]', '1', 1, 0, 'c') "CASE_SENSITIVE_MATCHING"  
  FROM  strings;