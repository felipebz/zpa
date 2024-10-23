-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/MERGE.html
var person_id  NUMBER;
var first_name VARCHAR2(20);
var last_name  VARCHAR2(20);
var title      VARCHAR2(10);
exec :person_id := 2;
exec :first_name := 'Alice';
exec :last_name := 'Jones';
exec :title := 'Mrs';
MERGE INTO people_target 
   ON (person_id = :person_id)
WHEN MATCHED THEN UPDATE
SET first_name = :first_name,
    last_name = :last_name,
    title = :title
WHEN NOT MATCHED THEN INSERT
    (person_id, first_name, last_name, title)
    VALUES (:person_id, :first_name, :last_name, :title);