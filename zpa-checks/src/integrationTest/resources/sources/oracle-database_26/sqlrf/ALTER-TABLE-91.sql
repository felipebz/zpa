-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-TABLE.html
ALTER TABLE employees
  ALLOCATE EXTENT (SIZE 5K INSTANCE 4);