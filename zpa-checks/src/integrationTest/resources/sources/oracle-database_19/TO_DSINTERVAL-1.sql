-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/TO_DSINTERVAL.html
SELECT TO_CHAR(TIMESTAMP '2009-01-01 00:00:00' + TO_DSINTERVAL('P100DT05H'),
   'YYYY-MM-DD HH24:MI:SS') "Time Stamp"
     FROM DUAL;