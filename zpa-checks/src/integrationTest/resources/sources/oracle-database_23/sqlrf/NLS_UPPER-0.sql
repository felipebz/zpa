-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/NLS_UPPER.html
SELECT NLS_UPPER('große') "Uppercase"
  FROM DUAL;
SELECT NLS_UPPER('große', 'NLS_SORT = XGerman') "Uppercase" 
  FROM DUAL;