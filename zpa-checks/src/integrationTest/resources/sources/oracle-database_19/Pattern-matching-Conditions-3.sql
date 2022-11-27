-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Pattern-matching-Conditions.html
SELECT salary 
    FROM employees 
    WHERE last_name = 'R%'
    ORDER BY salary;