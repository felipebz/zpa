-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/external-subprograms.html
SET SERVEROUTPUT ON;
DECLARE
  l_new_sal NUMBER;
  l_old_sal NUMBER;
  l_empNo NUMBER := 100;
BEGIN
  SELECT salary
  INTO l_old_sal
  FROM hr.employees
  WHERE employee_id = l_empNo;

DBMS_OUTPUT.PUT_LINE('Current salary for employee ' || l_empNo
                          || ' amounts to ' || l_old_sal);

l_new_sal := js_raise_sal(
  p_empno => l_empNo,
  p_percent => 10
);

DBMS_OUTPUT.PUT_LINE('New salary for employee ' || l_empNo
                          || ' increased to ' || l_new_sal);
END;
/