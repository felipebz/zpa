-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/TO_CHAR-datetime.html
SELECT hire_date "Default",  
       TO_CHAR(hire_date,'DS') "Short",  
       TO_CHAR(hire_date,'DL') "Long"FROM empl_temp  
WHERE employee_id IN (111, 112, 115);