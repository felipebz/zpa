-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-triggers.html
CREATE OR REPLACE TYPE nte
AUTHID DEFINER IS
OBJECT (
  emp_id     NUMBER(6),
  lastname   VARCHAR2(25),
  job        VARCHAR2(10),
  sal        NUMBER(8,2)
);
/
CREATE OR REPLACE TYPE emp_list_ IS
  TABLE OF nte;
/
CREATE OR REPLACE VIEW dept_view AS
  SELECT d.department_id, 
         d.department_name,
         CAST (MULTISET (SELECT e.employee_id, e.last_name, e.job_id, e.salary
                         FROM employees e
                         WHERE e.department_id = d.department_id
                        )
                        AS emp_list_
              ) emplist
  FROM departments d;
CREATE OR REPLACE TRIGGER dept_emplist_tr
  INSTEAD OF INSERT ON NESTED TABLE emplist OF dept_view
  REFERENCING NEW AS Employee
              PARENT AS Department
  FOR EACH ROW
BEGIN
  -- Insert on nested table translates to insert on base table:
  INSERT INTO employees (
    employee_id,
    last_name,
    email,
    hire_date,
    job_id,
    salary,
    department_id
  )
  VALUES (
    :Employee.emp_id,                      -- employee_id
    :Employee.lastname,                    -- last_name
    :Employee.lastname || '@example.com',  -- email
    SYSDATE,                               -- hire_date
    :Employee.job,                         -- job_id
    :Employee.sal,                         -- salary
    :Department.department_id              -- department_id
  );
END;
/