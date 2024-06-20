-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/TO_CHAR-number.html
SELECT To_char(employee_id) "NUM_TO_CHAR" 
FROM   empl_temp 
WHERE  employee_id IN ( 111, 112, 113, 115 );