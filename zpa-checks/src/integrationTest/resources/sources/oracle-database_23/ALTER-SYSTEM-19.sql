-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ALTER-SYSTEM.html
ALTER SYSTEM 
   SET DISPATCHERS = 
      '(INDEX=0)(PROTOCOL=TCP)(DISPATCHERS=5)',
      '(INDEX=1)(PROTOCOL=ipc)(DISPATCHERS=10)';