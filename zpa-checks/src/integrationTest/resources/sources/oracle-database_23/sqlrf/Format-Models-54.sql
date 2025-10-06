-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/Format-Models.html
SELECT TO_CHAR(SYSDATE, 'fmDay') || '''s Special' "Menu"
  FROM DUAL;