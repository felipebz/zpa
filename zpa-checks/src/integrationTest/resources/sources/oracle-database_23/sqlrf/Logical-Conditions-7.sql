-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/Logical-Conditions.html
SELECT employee_id FROM employees
   WHERE commission_pct = .4 OR salary > 20000
   ORDER BY employee_id;