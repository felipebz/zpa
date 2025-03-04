-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/dml-triggers.html
SELECT employee_id, count(*) c
  FROM employee_salaries
  GROUP BY employee_id
/