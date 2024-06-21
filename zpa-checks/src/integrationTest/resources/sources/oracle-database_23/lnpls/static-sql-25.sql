-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/static-sql.html
DECLARE
  CURSOR c (job VARCHAR2, max_sal NUMBER,
            hired DATE DEFAULT TO_DATE('31-DEC-1999', 'DD-MON-YYYY')) IS
    SELECT last_name, first_name, (salary - max_sal) overpayment
    FROM employees
    WHERE job_id = job
    AND salary > max_sal
    AND hire_date > hired
    ORDER BY salary;

  PROCEDURE print_overpaid IS
    last_name_   employees.last_name%TYPE;
    first_name_  employees.first_name%TYPE;
    overpayment_      employees.salary%TYPE;
  BEGIN
    LOOP
      FETCH c INTO last_name_, first_name_, overpayment_;
      EXIT WHEN c%NOTFOUND;
      DBMS_OUTPUT.PUT_LINE(last_name_ || ', ' || first_name_ ||
        ' (by ' || overpayment_ || ')');
    END LOOP;
  END print_overpaid;

BEGIN
  DBMS_OUTPUT.PUT_LINE('-------------------------------');
  DBMS_OUTPUT.PUT_LINE('Overpaid Sales Representatives:');
  DBMS_OUTPUT.PUT_LINE('-------------------------------');
  OPEN c('SA_REP', 10000);  -- existing reference
  print_overpaid; 
  CLOSE c;

  DBMS_OUTPUT.PUT_LINE('------------------------------------------------');
  DBMS_OUTPUT.PUT_LINE('Overpaid Sales Representatives Hired After 2014:');
  DBMS_OUTPUT.PUT_LINE('------------------------------------------------');
  OPEN c('SA_REP', 10000, TO_DATE('31-DEC-2014', 'DD-MON-YYYY'));
                          -- new reference
  print_overpaid; 
  CLOSE c;
END;
/