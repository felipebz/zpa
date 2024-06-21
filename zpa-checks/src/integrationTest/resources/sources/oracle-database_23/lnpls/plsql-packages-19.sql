-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-packages.html
CREATE PACKAGE emp_stuff AS
  CURSOR c1 RETURN employees%ROWTYPE;  -- Declare cursor
END emp_stuff;
/
CREATE PACKAGE BODY emp_stuff AS
  CURSOR c1 RETURN employees%ROWTYPE IS
    SELECT * FROM employees WHERE salary > 2500;  -- Define cursor
END emp_stuff;
/