-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ALTER-TABLE.html
ALTER TABLE staff 
    ADD (SCOPE FOR (dept) IS offices);