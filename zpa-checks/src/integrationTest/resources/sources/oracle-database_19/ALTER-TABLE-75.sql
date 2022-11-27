-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/ALTER-TABLE.html
ALTER TABLE employees
  ALLOCATE EXTENT (SIZE 5K INSTANCE 4);