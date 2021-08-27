-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/VSIZE.html
SELECT last_name, VSIZE (last_name) "BYTES"      
  FROM employees
  WHERE department_id = 10
  ORDER BY employee_id;