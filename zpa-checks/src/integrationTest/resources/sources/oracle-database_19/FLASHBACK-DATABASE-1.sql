-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/FLASHBACK-DATABASE.html
SHUTDOWN DATABASE
STARTUP MOUNT 
FLASHBACK DATABASE TO TIMESTAMP SYSDATE-1;