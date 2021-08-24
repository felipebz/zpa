SELECT employee_id FROM (SELECT employee_id+1 AS employee_id FROM employees)
   FOR UPDATE OF employee_id;
                 *
Error at line 2:
ORA-01733: virtual column not allowed here