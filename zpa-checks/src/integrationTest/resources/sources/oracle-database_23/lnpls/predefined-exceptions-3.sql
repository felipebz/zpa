-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/predefined-exceptions.html
CREATE OR REPLACE PACKAGE emp_dept_data AUTHID DEFINER AS
  TYPE cv_type IS REF CURSOR;

  PROCEDURE open_cv (
    cv       IN OUT cv_type,
    discrim  IN     POSITIVE
  );
  END emp_dept_data;
/
CREATE OR REPLACE PACKAGE BODY emp_dept_data AS
  PROCEDURE open_cv (
    cv      IN OUT cv_type,
    discrim IN     POSITIVE) IS
  BEGIN
    IF discrim = 1 THEN
    OPEN cv FOR
      SELECT * FROM EMPLOYEES ORDER BY employee_id;
    ELSIF discrim = 2 THEN
      OPEN cv FOR
        SELECT * FROM DEPARTMENTS ORDER BY department_id;
    END IF;
  END open_cv;
END emp_dept_data;
/