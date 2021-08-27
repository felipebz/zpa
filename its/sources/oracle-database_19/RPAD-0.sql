-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/RPAD.html
SELECT last_name, RPAD(' ', salary/1000/1, '*') "Salary"
   FROM employees
   WHERE department_id = 80
   ORDER BY last_name, "Salary";