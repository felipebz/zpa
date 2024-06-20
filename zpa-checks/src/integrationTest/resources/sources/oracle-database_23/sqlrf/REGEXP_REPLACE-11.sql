-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/REGEXP_REPLACE.html
WITH date_strings AS (   
  SELECT  '2015-01-01' d from dual union all   
  SELECT '2000-12-31' d from dual union all   
  SELECT '900-01-01' d from dual   
)   
  SELECT d "STRING",   
         regexp_replace(d, '([[:digit:]]+)-([[:digit:]]{2})-([[:digit:]]{2})', '\3.\2.\1') "MODIFIED_STRING"  
  FROM date_strings;