-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/SELECT.html
INSERT INTO TABLE(SELECT h.people FROM hr_info h
   WHERE h.department_id = 280)
   VALUES ('Smith', 280, 1750);