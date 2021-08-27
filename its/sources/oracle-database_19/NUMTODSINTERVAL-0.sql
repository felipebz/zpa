-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/NUMTODSINTERVAL.html
SELECT manager_id, last_name, hire_date,
       COUNT(*) OVER (PARTITION BY manager_id ORDER BY hire_date 
       RANGE NUMTODSINTERVAL(100, 'day') PRECEDING) AS t_count 
  FROM employees
  ORDER BY last_name, hire_date;