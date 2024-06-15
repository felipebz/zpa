-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ALTER-TABLE.html
ALTER TABLE students 
  MODIFY (last_name COLLATE BINARY_CI);