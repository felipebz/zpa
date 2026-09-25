-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/constraint.html
ALTER TABLE Product MODIFY (Name VARCHAR2(50) CHECK 
  (regexp_like(Name, '^Product')) PRECHECK);