-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/trigger-restrictions.html
DROP TABLE log;
CREATE TABLE log (
  emp_id  NUMBER(6),
  l_name  VARCHAR2(25),
  f_name  VARCHAR2(20)
);
CREATE OR REPLACE TRIGGER log_deletions
  AFTER DELETE ON employees
  FOR EACH ROW
DECLARE
  n INTEGER;
BEGIN
  INSERT INTO log VALUES (
    :OLD.employee_id,
    :OLD.last_name,
    :OLD.first_name
  );

  SELECT COUNT(*) INTO n FROM employees;
  DBMS_OUTPUT.PUT_LINE('There are now ' || n || ' employees.');
END;
/
DELETE FROM employees WHERE employee_id = 197;