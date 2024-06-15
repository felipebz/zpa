-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/LOCK-TABLE.html
LOCK TABLE employees
   IN EXCLUSIVE MODE 
   NOWAIT;