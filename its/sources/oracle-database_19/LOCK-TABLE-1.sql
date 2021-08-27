-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/LOCK-TABLE.html
LOCK TABLE employees@remote 
   IN SHARE MODE;