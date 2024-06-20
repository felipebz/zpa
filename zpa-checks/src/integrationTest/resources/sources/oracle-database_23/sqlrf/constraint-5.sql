-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/constraint.html
ALTER TABLE locations_demo
   MODIFY (country_id CONSTRAINT country_nn NOT NULL);