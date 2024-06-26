-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/TREAT.html
SELECT name, TREAT(VALUE(p) AS employee_t).salary salary 
   FROM persons p;