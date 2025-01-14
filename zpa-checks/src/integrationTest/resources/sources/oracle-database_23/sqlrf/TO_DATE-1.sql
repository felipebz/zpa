-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/TO_DATE.html
SELECT TO_DATE(
  'January 15, 1989, 11:00 A.M.',
  'Month dd, YYYY, HH:MI A.M.',
  'NLS_DATE_LANGUAGE = American')
    FROM DUAL;