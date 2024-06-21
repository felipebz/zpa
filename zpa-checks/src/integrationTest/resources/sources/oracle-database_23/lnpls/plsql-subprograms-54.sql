-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-subprograms.html
CREATE OR REPLACE FUNCTION compute_bonus (
  emp_id NUMBER,
  bonus NUMBER
) RETURN NUMBER
  AUTHID DEFINER
IS
  emp_sal NUMBER;
BEGIN
  SELECT salary INTO emp_sal
  FROM employees
  WHERE employee_id = emp_id;

  RETURN emp_sal + bonus;
END compute_bonus;
/
SELECT compute_bonus(120, 50) FROM DUAL;
SELECT compute_bonus(bonus => 50, emp_id => 120) FROM DUAL;
SELECT compute_bonus(120, bonus => 50) FROM DUAL;