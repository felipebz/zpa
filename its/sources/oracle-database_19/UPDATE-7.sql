-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/UPDATE.html
UPDATE employees
   SET salary = salary * 1.1
   WHERE department_id = 100
   RETURNING SUM(salary) INTO :bnd1;