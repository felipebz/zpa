-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-TABLE.html
ALTER TABLE staff 
   ADD (REF(dept) WITH ROWID);