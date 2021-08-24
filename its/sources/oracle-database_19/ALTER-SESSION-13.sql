ALTER SESSION
   ADVISE COMMIT; 

INSERT INTO employees@remote
   VALUES (8002, 'Juan', 'Fernandez', 'juanf@example.com', NULL, 
   TO_DATE('04-OCT-1992', 'DD-MON-YYYY'), 'SA_CLERK', 3000, 
   NULL, 121, 20); 

ALTER SESSION
   ADVISE ROLLBACK; 

DELETE FROM employees@local
   WHERE employee_id = 8002; 

COMMIT;