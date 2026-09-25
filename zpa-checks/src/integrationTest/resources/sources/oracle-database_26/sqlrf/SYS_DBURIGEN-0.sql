-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SYS_DBURIGEN.html
SELECT SYS_DBURIGEN(employee_id, email)
   FROM employees
   WHERE employee_id = 206;