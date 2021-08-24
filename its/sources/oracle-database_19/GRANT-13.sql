GRANT REFERENCES (employee_id), 
      UPDATE (employee_id, salary, commission_pct) 
   ON hr.employees
   TO oe;