-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/predefined-exceptions.html
DECLARE
  emp_rec   EMPLOYEES%ROWTYPE;
  dept_rec  DEPARTMENTS%ROWTYPE;
  cv        Emp_dept_data.CV_TYPE;
BEGIN
  emp_dept_data.open_cv(cv, 1);  -- Open cv for EMPLOYEES fetch.
  FETCH cv INTO dept_rec;        -- Fetch from DEPARTMENTS.
  DBMS_OUTPUT.PUT(dept_rec.DEPARTMENT_ID);
  DBMS_OUTPUT.PUT_LINE('  ' || dept_rec.LOCATION_ID);
EXCEPTION
  WHEN ROWTYPE_MISMATCH THEN
     BEGIN
       DBMS_OUTPUT.PUT_LINE
         ('Row type mismatch, fetching EMPLOYEES data ...');
       FETCH cv INTO emp_rec;
       DBMS_OUTPUT.PUT(emp_rec.DEPARTMENT_ID);
       DBMS_OUTPUT.PUT_LINE('  ' || emp_rec.LAST_NAME);
     END;
END;
/