-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/create-assertion.html
CREATE TABLE criminal_records (
  employee_id INTEGER NOT NULL,
  conviction_date DATE NOT NULL,
  PRIMARY KEY ( employee_id, conviction_date )
);