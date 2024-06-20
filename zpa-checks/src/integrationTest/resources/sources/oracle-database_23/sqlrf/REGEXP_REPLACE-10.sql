-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/REGEXP_REPLACE.html
WITH strings as (   
  SELECT 'AddressLine1' s FROM dual union all   
  SELECT 'ZipCode' s FROM dual union all   
  SELECT 'Country' s FROM dual   
)   
  SELECT s "STRING",  
         lower(regexp_replace(s, '([A-Z0-9])', '_\1', 2)) "MODIFIED_STRING"  
  FROM strings;