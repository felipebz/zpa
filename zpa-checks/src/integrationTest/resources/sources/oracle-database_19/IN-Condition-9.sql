-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/IN-Condition.html
SELECT v.employee_id, v.last_name, v.lev FROM
      (SELECT employee_id, last_name, LEVEL lev 
      FROM employees v
      START WITH employee_id = 100 
      CONNECT BY PRIOR employee_id = manager_id) v 
   WHERE (v.employee_id, v.lev) IN
      (SELECT employee_id, 2 FROM employees);