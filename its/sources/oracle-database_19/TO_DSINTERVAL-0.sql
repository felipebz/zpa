-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/TO_DSINTERVAL.html
SELECT employee_id, last_name FROM employees
   WHERE hire_date + TO_DSINTERVAL('100 00:00:00')
   <= DATE '2002-11-01'
   ORDER BY employee_id;