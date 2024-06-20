-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/TO_CHAR-character.html
SELECT To_char(clob_column) "CLOB_TO_CHAR" 
FROM   empl_temp 
WHERE  employee_id IN ( 111, 112, 115 );