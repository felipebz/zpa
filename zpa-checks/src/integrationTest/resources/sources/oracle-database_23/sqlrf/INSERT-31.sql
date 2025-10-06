-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/INSERT.html
CREATE TABLE people ( 
  person_id   INTEGER NOT NULL PRIMARY KEY, 
  given_name  VARCHAR2(100) NOT NULL, 
  family_name VARCHAR2(100) NOT NULL, 
  title       VARCHAR2(20), 
  birth_date  DATE 
);
CREATE TABLE patients ( 
  patient_id          INTEGER NOT NULL PRIMARY KEY REFERENCES people (person_id), 
  last_admission_date DATE 
);
CREATE TABLE staff ( 
  staff_id   INTEGER NOT NULL PRIMARY KEY REFERENCES people (person_id), 
  hired_date DATE 
);