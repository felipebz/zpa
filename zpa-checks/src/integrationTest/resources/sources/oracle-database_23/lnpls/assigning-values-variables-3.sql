-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/assigning-values-variables.html
DECLARE
  bonus   NUMBER(8,2);
BEGIN
  SELECT salary * 0.10 INTO bonus
  FROM employees
  WHERE employee_id = 100;

  DBMS_OUTPUT.PUT_LINE('bonus = ' || TO_CHAR(bonus));
END;

/