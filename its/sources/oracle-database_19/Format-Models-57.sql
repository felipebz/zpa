-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Format-Models.html
UPDATE employees 
  SET hire_date = TO_DATE('2008 05 20','YYYY MM DD') 
  WHERE last_name = 'Hunold';