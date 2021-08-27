-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/INSERT.html
INSERT INTO bonuses
   SELECT employee_id, salary*1.1 
   FROM employees
   WHERE commission_pct > 0.25;