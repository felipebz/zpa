-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/TO_YMINTERVAL.html
SELECT TO_YMINTERVAL('1x-02'
       DEFAULT '00-00' ON CONVERSION ERROR) "Value"
  FROM DUAL;