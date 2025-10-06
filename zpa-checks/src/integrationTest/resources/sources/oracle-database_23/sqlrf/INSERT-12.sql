-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/INSERT.html
INSERT INTO employees@remote
   VALUES (8002, 'Juan', 'Fernandez', 'juanf@example.com', NULL, 
   TO_DATE('04-OCT-1992', 'DD-MON-YYYY'), 'SH_CLERK', 3000, 
   NULL, 121, 20);