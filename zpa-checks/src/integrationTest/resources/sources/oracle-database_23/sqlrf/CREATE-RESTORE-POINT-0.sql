-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-RESTORE-POINT.html
CREATE RESTORE POINT good_data;
SELECT salary FROM employees WHERE employee_id = 108;
UPDATE employees SET salary = salary*10
   WHERE employee_id = 108;
SELECT salary FROM employees
   WHERE employee_id = 108;
COMMIT;
FLASHBACK TABLE employees TO RESTORE POINT good_data;
SELECT salary FROM employees
   WHERE employee_id = 108;