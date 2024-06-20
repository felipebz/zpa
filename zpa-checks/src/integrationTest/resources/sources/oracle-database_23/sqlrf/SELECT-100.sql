-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/SELECT.html
UPDATE TABLE(SELECT h.people FROM hr_info h
   WHERE h.department_id = 280) p
   SET p.salary = p.salary + 100;