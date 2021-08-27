-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/CORR.html
SELECT employee_id, job_id, 
       TO_CHAR((SYSDATE - hire_date) YEAR TO MONTH ) "Yrs-Mns",     salary, 
       CORR(SYSDATE-hire_date, salary)
       OVER(PARTITION BY job_id) AS "Correlation"
  FROM employees
  WHERE department_id in (50, 80)
  ORDER BY job_id, employee_id;