-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/MONTHS_BETWEEN.html
SELECT MONTHS_BETWEEN
       (TO_DATE('02-02-1995','MM-DD-YYYY'),
        TO_DATE('01-01-1995','MM-DD-YYYY') ) "Months"
  FROM DUAL;