-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ALTER-TABLE.html
ALTER TABLE employees
  ALLOCATE EXTENT (SIZE 5K INSTANCE 4);