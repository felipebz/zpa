-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-source-text-wrapping.html
DECLARE
  s employees.salary%TYPE;
BEGIN
  SELECT salary INTO s FROM employees WHERE employee_id=130;
  DBMS_OUTPUT.PUT_LINE('Old salary: ' || s);
  emp_actions.raise_salary(130, 100);
  SELECT salary INTO s FROM employees WHERE employee_id=130;
  DBMS_OUTPUT.PUT_LINE('New salary: ' || s);
END;
/