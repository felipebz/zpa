-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/RANK.html
SELECT department_id, last_name, salary,
       RANK() OVER (PARTITION BY department_id ORDER BY salary) RANK
  FROM employees WHERE department_id = 60
  ORDER BY RANK, last_name;