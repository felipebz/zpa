-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/MAX.html
SELECT manager_id, last_name, salary
  FROM (SELECT manager_id, last_name, salary, 
               MAX(salary) OVER (PARTITION BY manager_id) AS rmax_sal
          FROM employees)
  WHERE salary = rmax_sal
  ORDER BY manager_id, last_name, salary;