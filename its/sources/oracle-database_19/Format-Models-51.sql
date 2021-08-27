-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Format-Models.html
SELECT TO_CHAR(SYSDATE, 'fmDDTH') || ' of ' ||
       TO_CHAR(SYSDATE, 'fmMonth') || ', ' ||
       TO_CHAR(SYSDATE, 'YYYY') "Ides" 
  FROM DUAL; 