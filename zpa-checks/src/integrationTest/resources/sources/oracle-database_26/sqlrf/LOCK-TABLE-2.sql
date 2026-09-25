-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/LOCK-TABLE.html
LOCK TABLE employees@remote 
   IN SHARE MODE;