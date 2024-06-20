-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/DUMP.html
SELECT DUMP('abc', 1016)
  FROM DUAL;
SELECT DUMP(last_name, 8, 3, 2) "OCTAL"
  FROM employees
  WHERE last_name = 'Hunold'
  ORDER BY employee_id;
SELECT DUMP(last_name, 10, 3, 2) "ASCII"
  FROM employees
  WHERE last_name = 'Hunold'
  ORDER BY employee_id;