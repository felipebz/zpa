-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/RAWTONHEX.html
SELECT RAWTONHEX(raw_column),
   DUMP ( RAWTONHEX (raw_column) ) "DUMP" 
   FROM graphics; 