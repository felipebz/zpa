-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/GRANT.html
GRANT REFERENCES (employee_id), 
      UPDATE (employee_id, salary, commission_pct) 
   ON hr.employees
   TO oe;