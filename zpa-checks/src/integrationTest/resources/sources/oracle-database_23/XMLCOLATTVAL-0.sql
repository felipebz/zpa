-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/XMLCOLATTVAL.html
SELECT XMLELEMENT("Emp",
   XMLCOLATTVAL(e.employee_id, e.last_name, e.salary)) "Emp Element"
   FROM employees e
   WHERE employee_id = 204;