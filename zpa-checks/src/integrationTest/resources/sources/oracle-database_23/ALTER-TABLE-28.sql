-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ALTER-TABLE.html
ALTER TABLE employees ADD CONSTRAINT check_comp 
   CHECK (salary + (commission_pct*salary) <= 5000)
   DISABLE;