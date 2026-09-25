-- https://docs.oracle.com/en/database/oracle/oracle-database/26/lnpls/dml-triggers.html
UPDATE employees
  SET salary = salary * 1.1
  WHERE department_id = 50
/