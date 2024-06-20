-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/MERGE.html
var person_id  NUMBER;
var first_name VARCHAR2(20);
var last_name  VARCHAR2(20);
var title      VARCHAR2(10);
exec :person_id := 3;
exec :first_name := 'Gerald';
exec :last_name := 'Walker';
exec :title := 'Mr';
MERGE INTO people_target pt 
   USING (SELECT :person_id  AS person_id,
                 :first_name AS first_name,
                 :last_name  AS last_name,
                 :title      AS title FROM DUAL) ps
   ON (pt.person_id = ps.person_id)
WHEN MATCHED THEN UPDATE
SET pt.first_name = ps.first_name, 
    pt.last_name = ps.last_name, 
    pt.title = ps.title 
WHEN NOT MATCHED THEN INSERT
    (pt.person_id, pt.first_name, pt.last_name, pt.title) 
    VALUES (ps.person_id, ps.first_name, ps.last_name, ps.title);