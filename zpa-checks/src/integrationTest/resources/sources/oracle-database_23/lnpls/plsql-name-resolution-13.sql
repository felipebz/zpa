-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-name-resolution.html
DECLARE
  FUNCTION dept_name (department_id IN NUMBER)
    RETURN departments.department_name%TYPE
  IS
    department_name  departments.department_name%TYPE;
  BEGIN
    SELECT department_name INTO dept_name.department_name
      --    ^column               ^local variable
    FROM departments
    WHERE department_id = dept_name.department_id;
    --     ^column          ^formal parameter
    RETURN department_name;
  END dept_name;
BEGIN
  FOR item IN (
    SELECT department_id
    FROM departments
    ORDER BY department_name) LOOP

      DBMS_OUTPUT.PUT_LINE ('Department: ' || dept_name(item.department_id));
  END LOOP;
END;
/