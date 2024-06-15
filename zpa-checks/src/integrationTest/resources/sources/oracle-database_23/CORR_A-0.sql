-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CORR_A.html
SELECT COUNT(*) count,
       CORR_S(salary, commission_pct) commission,
       CORR_S(salary, employee_id) empid
  FROM employees;