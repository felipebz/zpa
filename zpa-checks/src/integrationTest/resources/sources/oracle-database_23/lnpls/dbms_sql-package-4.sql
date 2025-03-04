-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/dbms_sql-package.html
CREATE OR REPLACE PROCEDURE get_employee_info (id IN VARCHAR2) AUTHID DEFINER AS
  rc  SYS_REFCURSOR;
BEGIN
  -- Return employee info

  OPEN rc FOR SELECT first_name, last_name, email, phone_number
              FROM employees
              WHERE employee_id = id;
  DBMS_SQL.RETURN_RESULT(rc);

  -- Return employee job history

  OPEN RC FOR SELECT job_title, start_date, end_date
              FROM job_history jh, jobs j
              WHERE jh.employee_id = id AND
                    jh.job_id = j.job_id
              ORDER BY start_date DESC;
  DBMS_SQL.RETURN_RESULT(rc);
END;
/
<<main>>
DECLARE
  c            INTEGER;
  rc           SYS_REFCURSOR;
  n            NUMBER;

  first_name   VARCHAR2(20);
  last_name    VARCHAR2(25);
  email        VARCHAR2(25);
  phone_number VARCHAR2(20);

  job_title    VARCHAR2(35);
  start_date   DATE;
  end_date     DATE;

BEGIN

  c := DBMS_SQL.OPEN_CURSOR(true);
  DBMS_SQL.PARSE(c, 'BEGIN get_employee_info(:id); END;', DBMS_SQL.NATIVE);
  DBMS_SQL.BIND_VARIABLE(c, ':id', 176);
  n := DBMS_SQL.EXECUTE(c);

  -- Get employee info

  dbms_sql.get_next_result(c, rc);
  FETCH rc INTO first_name, last_name, email, phone_number;

  DBMS_OUTPUT.PUT_LINE('Employee: '||first_name || ' ' || last_name);
  DBMS_OUTPUT.PUT_LINE('Email: ' ||email);
  DBMS_OUTPUT.PUT_LINE('Phone: ' ||phone_number);

  -- Get employee job history

  DBMS_OUTPUT.PUT_LINE('Titles:');
  DBMS_SQL.GET_NEXT_RESULT(c, rc);
  LOOP
    FETCH rc INTO job_title, start_date, end_date;
    EXIT WHEN rc%NOTFOUND;
    DBMS_OUTPUT.PUT_LINE
      ('- '||job_title||' ('||start_date||' - ' ||end_date||')');
  END LOOP;

  DBMS_SQL.CLOSE_CURSOR(c);
END main;
/