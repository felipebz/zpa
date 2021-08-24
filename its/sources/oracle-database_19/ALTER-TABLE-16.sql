ALTER TABLE employees ADD CONSTRAINT check_comp 
   CHECK (salary + (commission_pct*salary) <= 5000)
   DISABLE;