-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/IS-OF-type-Condition.html
SELECT * FROM persons p 
   WHERE VALUE(p) IS OF TYPE (employee_t);