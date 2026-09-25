-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-TABLE.html
ALTER TABLE product MODIFY (name VARCHAR2(50) CHECK 
  (regexp_like(name, '^Product')) PRECHECK);