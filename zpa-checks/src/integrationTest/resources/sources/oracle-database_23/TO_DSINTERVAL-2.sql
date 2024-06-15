-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/TO_DSINTERVAL.html
SELECT TO_DSINTERVAL('1o 1:02:10'
       DEFAULT '10 8:00:00' ON CONVERSION ERROR) "Value"
  FROM DUAL;