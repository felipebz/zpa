-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/transaction-processing-and-control.html
DROP TABLE emp;
CREATE TABLE emp AS SELECT * FROM employees;
DECLARE
  CURSOR c1 IS
    SELECT last_name, job_id, rowid
    FROM emp;  -- no FOR UPDATE clause

  my_lastname   employees.last_name%TYPE;
  my_jobid      employees.job_id%TYPE;
  my_rowid      UROWID;
BEGIN
  OPEN c1;
  LOOP
    FETCH c1 INTO my_lastname, my_jobid, my_rowid;
    EXIT WHEN c1%NOTFOUND;

    UPDATE emp
    SET salary = salary * 1.02
    WHERE rowid = my_rowid;  -- simulates WHERE CURRENT OF c1

    COMMIT;
  END LOOP;
  CLOSE c1;
END;
/