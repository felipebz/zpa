-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/cursor-variables.html
BEGIN
  OPEN :emp_cv FOR SELECT * FROM employees;
  OPEN :dept_cv FOR SELECT * FROM departments;
  OPEN :loc_cv FOR SELECT * FROM locations;
END;
/