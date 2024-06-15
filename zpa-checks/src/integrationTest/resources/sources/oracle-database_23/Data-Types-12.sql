-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/Data-Types.html
SELECT TO_DATE('29-FEB-2004', 'DD-MON-YYYY') + TO_YMINTERVAL('4-0')
  FROM DUAL;