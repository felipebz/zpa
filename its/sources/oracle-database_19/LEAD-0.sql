-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/LEAD.html
SELECT hire_date, last_name,
       LEAD(hire_date, 1) OVER (ORDER BY hire_date) AS "NextHired" 
  FROM employees
  WHERE department_id = 30
  ORDER BY hire_date;