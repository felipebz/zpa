-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/INSERT.html
INSERT INTO people (person_id, given_name, family_name, title) 
  WITH names AS ( 
    SELECT 4, 'Ruth',     'Fox' family_name,      'Mrs'    FROM dual UNION ALL 
    SELECT 5, 'Isabelle', 'Squirrel' family_name, 'Miss'   FROM dual UNION ALL 
    SELECT 6, 'Justin',   'Frog' family_name,     'Master' FROM dual UNION ALL 
    SELECT 7, 'Lisa',     'Owl' family_name,      'Dr'     FROM dual 
  ) 
  SELECT * FROM names 
  WHERE  family_name LIKE 'F%';