-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/COUNT.html
SELECT last_name, salary,
       COUNT(*) OVER (ORDER BY salary RANGE BETWEEN 50 PRECEDING AND
                      150 FOLLOWING) AS mov_count
  FROM employees
  ORDER BY salary, last_name;