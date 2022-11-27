-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/TO_TIMESTAMP.html
SELECT TO_TIMESTAMP ('10-Sept-02 14:10:10.123000'
       DEFAULT NULL ON CONVERSION ERROR,
       'DD-Mon-RR HH24:MI:SS.FF',
       'NLS_DATE_LANGUAGE = American') "Value"
  FROM DUAL;