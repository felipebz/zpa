-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/exception-handling-triggers.html
CREATE OR REPLACE PROCEDURE insert_row_proc AUTHID CURRENT_USER AS
  no_remote_db EXCEPTION;  -- declare exception
  PRAGMA EXCEPTION_INIT (no_remote_db, -20000);
                           -- assign error code to exception
BEGIN
  INSERT INTO employees@remote (
    employee_id, first_name, last_name, email, hire_date, job_id
  ) 
  VALUES (
    99, 'Jane', 'Doe', 'jane.doe@example.com', SYSDATE, 'ST_MAN'
  );
EXCEPTION
  WHEN OTHERS THEN
    INSERT INTO emp_log (Emp_id, Log_date, New_salary, Action)
      VALUES (99, SYSDATE, NULL, 'Could not insert row.');

  RAISE_APPLICATION_ERROR (-20000, 'Remote database is unavailable.');
END;
/
CREATE OR REPLACE TRIGGER employees_tr
  AFTER INSERT ON employees
  FOR EACH ROW
BEGIN
  insert_row_proc;
END;
/