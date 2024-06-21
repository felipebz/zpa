-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-optimization-and-tuning.html
DROP TABLE emp_temp;
CREATE TABLE emp_temp AS
SELECT * FROM employees
ORDER BY employee_id;
DECLARE
  TYPE SalList IS TABLE OF employees.salary%TYPE;
  old_sals SalList;
  new_sals SalList;
  TYPE NameList IS TABLE OF employees.last_name%TYPE;
  names NameList;
BEGIN
  UPDATE emp_temp SET salary = salary * 1.15
  WHERE salary < 2500
  RETURNING OLD salary, NEW salary, last_name 
  BULK COLLECT INTO old_sals, new_sals, names; 

  DBMS_OUTPUT.PUT_LINE('Updated ' || SQL%ROWCOUNT || ' rows: ');
  FOR i IN old_sals.FIRST .. old_sals.LAST
  LOOP
    DBMS_OUTPUT.PUT_LINE(names(i) || ': Old Salary $' || old_sals(i) || 
            ', New Salary $' || new_sals(i));
  END LOOP;
END;
/