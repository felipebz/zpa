-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CASE-Expressions.html
SELECT AVG(CASE WHEN e.salary > 2000 THEN e.salary
   ELSE 2000 END) "Average Salary" FROM employees e;