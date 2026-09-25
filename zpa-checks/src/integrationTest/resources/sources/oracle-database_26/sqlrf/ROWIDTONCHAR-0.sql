-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ROWIDTONCHAR.html
SELECT LENGTHB( ROWIDTONCHAR(ROWID) ) Length, ROWIDTONCHAR(ROWID) 
   FROM employees
   ORDER BY length;