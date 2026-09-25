-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Format-Models.html
SELECT TO_CHAR(SYSDATE, 'fmDay') || '''s Special' "Menu"
  FROM DUAL;