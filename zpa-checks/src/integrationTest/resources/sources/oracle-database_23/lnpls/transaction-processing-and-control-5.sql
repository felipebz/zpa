-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/transaction-processing-and-control.html
DROP TABLE emp_name;
CREATE TABLE emp_name AS 
  SELECT employee_id, last_name
  FROM employees;
CREATE UNIQUE INDEX empname_ix
ON emp_name (employee_id);
DROP TABLE emp_sal;
CREATE TABLE emp_sal AS
  SELECT employee_id, salary
  FROM employees;
CREATE UNIQUE INDEX empsal_ix
ON emp_sal (employee_id);
DROP TABLE emp_job;
CREATE TABLE emp_job AS
  SELECT employee_id, job_id
  FROM employees;
CREATE UNIQUE INDEX empjobid_ix
ON emp_job (employee_id);
DECLARE
  emp_id        NUMBER(6);
  emp_lastname  VARCHAR2(25);
  emp_salary    NUMBER(8,2);
  emp_jobid     VARCHAR2(10);
BEGIN
  SELECT employee_id, last_name, salary, job_id
  INTO emp_id, emp_lastname, emp_salary, emp_jobid
  FROM employees
  WHERE employee_id = 120;

  INSERT INTO emp_name (employee_id, last_name)
  VALUES (emp_id, emp_lastname);

  INSERT INTO emp_sal (employee_id, salary) 
  VALUES (emp_id, emp_salary);

  INSERT INTO emp_job (employee_id, job_id)
  VALUES (emp_id, emp_jobid);

EXCEPTION
  WHEN DUP_VAL_ON_INDEX THEN
    ROLLBACK;
    DBMS_OUTPUT.PUT_LINE('Inserts were rolled back');
END;
/