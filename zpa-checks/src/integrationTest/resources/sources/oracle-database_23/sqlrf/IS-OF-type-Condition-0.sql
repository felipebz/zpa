-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/IS-OF-type-Condition.html
SELECT * FROM persons p 
   WHERE VALUE(p) IS OF TYPE (employee_t);
SELECT * FROM persons p 
   WHERE VALUE(p) IS OF (ONLY part_time_emp_t);