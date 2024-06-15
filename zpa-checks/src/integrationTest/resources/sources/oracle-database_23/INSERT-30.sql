-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/INSERT.html
INSERT INTO people (person_id, given_name, family_name, title) 
VALUES (3, 'Dave', 'Frog', (SELECT 'Mr' FROM dual));