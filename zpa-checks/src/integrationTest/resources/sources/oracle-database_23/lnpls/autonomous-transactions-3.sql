-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/autonomous-transactions.html
DROP TABLE emp;
CREATE TABLE emp AS SELECT * FROM employees;
DECLARE
  PRAGMA AUTONOMOUS_TRANSACTION;
  emp_id NUMBER(6)   := 200;
  amount NUMBER(6,2) := 200;
BEGIN
  UPDATE employees
  SET salary =  salary - amount
  WHERE employee_id = emp_id;

  COMMIT;
END;
/