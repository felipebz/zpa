-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ROWIDTONCHAR.html
SELECT LENGTHB( ROWIDTONCHAR(ROWID) ) Length, ROWIDTONCHAR(ROWID) 
   FROM employees
   ORDER BY length;