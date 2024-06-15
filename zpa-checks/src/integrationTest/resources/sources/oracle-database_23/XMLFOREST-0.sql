-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/XMLFOREST.html
SELECT XMLELEMENT("Emp", 
   XMLFOREST(e.employee_id, e.last_name, e.salary))
   "Emp Element"
   FROM employees e WHERE employee_id = 204;