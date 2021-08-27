-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/TO_CHAR-datetime.html
WITH nums AS (  
  SELECT 10 n FROM dual union  
  SELECT 9.99 n FROM dual union  
  SELECT 1000000 n FROM dual  --one million  
)  
SELECT n "Input Number N",  
       to_char(n),  
       to_char(n, '9,999,999.99') "Number with Commas",  
       to_char(n, '0,000,000.000') "Zero-padded Number",  
       to_char(n, '9.9EEEE') "Scientific Notation"  
FROM nums;