-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/SELECT.html
DELETE TABLE(SELECT h.people FROM hr_info h
   WHERE h.department_id = 280) p
   WHERE p.salary > 1700;