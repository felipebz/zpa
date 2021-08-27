-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/XMLELEMENT.html
SELECT XMLELEMENT("Emp", XMLELEMENT("Name", 
   e.job_id||' '||e.last_name),
   XMLELEMENT("Hiredate", e.hire_date)) as "Result"
   FROM employees e WHERE employee_id > 200;