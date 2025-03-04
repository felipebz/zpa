-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/autonomous-transactions.html
DROP TABLE emp;
CREATE TABLE emp AS SELECT * FROM employees;
DROP TABLE log;
CREATE TABLE log (
  log_id   NUMBER(6),
  up_date  DATE,
  new_sal  NUMBER(8,2),
  old_sal  NUMBER(8,2)
);
CREATE OR REPLACE TRIGGER log_sal
  BEFORE UPDATE OF salary ON emp FOR EACH ROW
DECLARE
  PRAGMA AUTONOMOUS_TRANSACTION;
BEGIN
  INSERT INTO log (
    log_id,
    up_date,
    new_sal,
    old_sal
  )
  VALUES (
    :old.employee_id,
    SYSDATE,
    :new.salary,
    :old.salary
  );
  COMMIT;
END;
/
UPDATE emp
SET salary = salary * 1.05
WHERE employee_id = 115;
COMMIT;
UPDATE emp
SET salary = salary * 1.05
WHERE employee_id = 116;
ROLLBACK;
SELECT * FROM log
WHERE log_id = 115 OR log_id = 116;