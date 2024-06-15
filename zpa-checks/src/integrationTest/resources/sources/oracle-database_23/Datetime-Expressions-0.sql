-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/Datetime-Expressions.html
SELECT FROM_TZ(CAST(TO_DATE('1999-12-01 11:00:00', 
      'YYYY-MM-DD HH:MI:SS') AS TIMESTAMP), 'America/New_York') 
   AT TIME ZONE 'America/Los_Angeles' "West Coast Time" 
   FROM DUAL;