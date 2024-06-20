-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/MERGE.html
CREATE TABLE people_source ( 
  person_id  INTEGER NOT NULL PRIMARY KEY, 
  first_name VARCHAR2(20) NOT NULL, 
  last_name  VARCHAR2(20) NOT NULL, 
  title      VARCHAR2(10) NOT NULL 
);
CREATE TABLE people_target ( 
  person_id  INTEGER NOT NULL PRIMARY KEY, 
  first_name VARCHAR2(20) NOT NULL, 
  last_name  VARCHAR2(20) NOT NULL, 
  title      VARCHAR2(10) NOT NULL 
);
INSERT INTO people_target VALUES (1, 'John', 'Smith', 'Mr');
INSERT INTO people_target VALUES (2, 'alice', 'jones', 'Mrs');
INSERT INTO people_source VALUES (2, 'Alice', 'Jones', 'Mrs.');
INSERT INTO people_source VALUES (3, 'Jane', 'Doe', 'Miss');
INSERT INTO people_source VALUES (4, 'Dave', 'Brown', 'Mr');
COMMIT;