-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-INDEX.html
CREATE INDEX income_ix 
   ON employees(salary + (salary*commission_pct));
SELECT first_name||' '||last_name "Name"
   FROM employees 
   WHERE (salary*commission_pct) + salary > 15000
   ORDER BY employee_id;