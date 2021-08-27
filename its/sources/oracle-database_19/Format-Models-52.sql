-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Format-Models.html
SELECT TO_CHAR(SYSDATE, 'DDTH') || ' of ' ||
   TO_CHAR(SYSDATE, 'Month') || ', ' ||
   TO_CHAR(SYSDATE, 'YYYY') "Ides"
  FROM DUAL; 