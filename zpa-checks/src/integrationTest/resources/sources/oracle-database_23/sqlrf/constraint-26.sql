-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/constraint.html
CREATE TYPE person_name AS OBJECT
   (first_name VARCHAR2(30), last_name VARCHAR2(30));
/
CREATE TABLE students (name person_name, age INTEGER,
   CHECK (name.first_name IS NOT NULL AND 
          name.last_name IS NOT NULL));