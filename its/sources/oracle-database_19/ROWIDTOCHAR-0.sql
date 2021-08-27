-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/ROWIDTOCHAR.html
SELECT ROWID FROM employees 
   WHERE ROWIDTOCHAR(ROWID) LIKE '%JAAB%'
   ORDER BY ROWID;