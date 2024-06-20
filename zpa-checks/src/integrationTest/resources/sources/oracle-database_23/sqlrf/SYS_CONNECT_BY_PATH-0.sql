-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/SYS_CONNECT_BY_PATH.html
SELECT LPAD(' ', 2*level-1)||SYS_CONNECT_BY_PATH(last_name, '/') "Path"
   FROM employees
   START WITH last_name = 'Kochhar'
   CONNECT BY PRIOR employee_id = manager_id;