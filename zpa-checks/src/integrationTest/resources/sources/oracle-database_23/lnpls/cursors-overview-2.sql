-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/cursors-overview.html
DROP TABLE employees_temp;
CREATE TABLE employees_temp AS
  SELECT * FROM employees;
DECLARE
  mgr_no NUMBER(6) := 122;
BEGIN
  DELETE FROM employees_temp WHERE manager_id = mgr_no;
  DBMS_OUTPUT.PUT_LINE
    ('Number of employees deleted: ' || TO_CHAR(SQL%ROWCOUNT));
END;
/