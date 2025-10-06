-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/INSERT.html
INSERT INTO people (person_id, given_name, family_name, title) 
  WITH names AS ( 
    SELECT 4, 'Ruth',     'Fox',      'Mrs'    FROM dual UNION ALL 
    SELECT 5, 'Isabelle', 'Squirrel', 'Miss'   FROM dual UNION ALL 
    SELECT 6, 'Justin',   'Frog',     'Master' FROM dual UNION ALL 
    SELECT 7, 'Lisa',     'Owl',      'Dr'     FROM dual 
  ) 
  SELECT * FROM names;