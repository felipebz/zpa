INSERT INTO people (person_id, given_name, family_name, title) 
VALUES (3, 'Dave', 'Frog', (SELECT 'Mr' FROM dual));