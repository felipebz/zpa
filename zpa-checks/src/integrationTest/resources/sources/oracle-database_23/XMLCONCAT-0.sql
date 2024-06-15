-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/XMLCONCAT.html
SELECT XMLCONCAT(XMLELEMENT("First", e.first_name),
   XMLELEMENT("Last", e.last_name)) AS "Result"
   FROM employees e
   WHERE e.employee_id > 202;