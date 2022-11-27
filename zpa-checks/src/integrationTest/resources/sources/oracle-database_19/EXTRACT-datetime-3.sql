-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/EXTRACT-datetime.html
SELECT EXTRACT(TIMEZONE_REGION FROM TIMESTAMP '1999-01-01 10:00:00 -08:00')
  FROM DUAL;