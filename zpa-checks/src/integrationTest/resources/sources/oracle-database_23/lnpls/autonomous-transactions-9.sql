-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/autonomous-transactions.html
DROP TABLE debug_output;
CREATE TABLE debug_output (message VARCHAR2(200));
CREATE OR REPLACE PACKAGE debugging AUTHID DEFINER AS
  FUNCTION log_msg (msg VARCHAR2) RETURN VARCHAR2;
END debugging;
/
CREATE OR REPLACE PACKAGE BODY debugging AS
  FUNCTION log_msg (msg VARCHAR2) RETURN VARCHAR2 IS
    PRAGMA AUTONOMOUS_TRANSACTION;
  BEGIN
    INSERT INTO debug_output (message) VALUES (msg);
    COMMIT;
    RETURN msg;
  END;
END debugging;
/
DECLARE
  my_emp_id    NUMBER(6);
  my_last_name VARCHAR2(25);
  my_count     NUMBER;
BEGIN
  my_emp_id := 120;

  SELECT debugging.log_msg(last_name)
  INTO my_last_name
  FROM employees
  WHERE employee_id = my_emp_id;

  /* Even if you roll back in this scope,
     the insert into 'debug_output' remains committed,
     because it is part of an autonomous transaction. */

  ROLLBACK;
END;
/