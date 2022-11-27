-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/ALTER-TABLE.html
ALTER TABLE staff 
    ADD (SCOPE FOR (dept) IS offices);