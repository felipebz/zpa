-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json-data-type.html
SELECT * 
  FROM emp t
  WHERE t.data.dept = 'SALES' ORDER BY t.data.name