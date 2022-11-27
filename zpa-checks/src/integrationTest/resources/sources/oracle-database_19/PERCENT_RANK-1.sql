-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/PERCENT_RANK.html
SELECT department_id, last_name, salary, PERCENT_RANK() 
       OVER (PARTITION BY department_id ORDER BY salary DESC) AS pr
  FROM employees
  ORDER BY pr, salary, last_name;