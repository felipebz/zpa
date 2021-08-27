-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/INSERT.html
INSERT ALL 
  /* Everyone is a person, so insert all rows into people */ 
  WHEN 1=1 THEN 
    INTO people (person_id, given_name, family_name, title) 
    VALUES (id, given_name, family_name, title) 
  /* Only people with an admission date are patients */ 
  WHEN admission_date IS NOT NULL THEN 
    INTO patients (patient_id, last_admission_date) 
    VALUES (id, admission_date) 
  /* Only people with a hired date are staff */ 
  WHEN hired_date IS NOT NULL THEN 
    INTO staff (staff_id, hired_date) 
    VALUES (id, hired_date) 
  WITH names AS ( 
    SELECT 4 id, 'Ruth' given_name, 'Fox' family_name, 'Mrs' title, 
           NULL hired_date, DATE'2009-12-31' admission_date 
    FROM   dual UNION ALL 
    SELECT 5 id, 'Isabelle' given_name, 'Squirrel' family_name, 'Miss' title , 
           NULL hired_date, DATE'2014-01-01' admission_date 
    FROM   dual UNION ALL 
    SELECT 6 id, 'Justin' given_name, 'Frog' family_name, 'Master' title, 
           NULL hired_date, DATE'2015-04-22' admission_date 
    FROM   dual UNION ALL 
    SELECT 7 id, 'Lisa' given_name, 'Owl' family_name, 'Dr' title, 
           DATE'2015-01-01' hired_date, NULL admission_date 
    FROM   dual 
  ) 
  SELECT * FROM names;