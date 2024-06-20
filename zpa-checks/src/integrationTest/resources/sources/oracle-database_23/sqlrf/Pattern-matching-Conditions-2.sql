-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/Pattern-matching-Conditions.html
SELECT salary 
    FROM employees
    WHERE last_name LIKE 'R%'
    ORDER BY salary;