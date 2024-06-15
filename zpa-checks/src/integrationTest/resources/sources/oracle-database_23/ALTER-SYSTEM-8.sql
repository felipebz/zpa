-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ALTER-SYSTEM.html
ALTER SYSTEM ARCHIVE LOG 
    LOGFILE 'diskl:log6.log' 
    TO 'diska:[arch$]';