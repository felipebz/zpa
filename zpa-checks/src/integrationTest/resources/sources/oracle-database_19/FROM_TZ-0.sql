-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/FROM_TZ.html
SELECT FROM_TZ(TIMESTAMP '2000-03-28 08:00:00', '3:00') 
  FROM DUAL;