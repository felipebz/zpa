-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ALTER-SESSION.html
ALTER SESSION 
   SET NLS_DATE_LANGUAGE = French;

SELECT TO_CHAR(SYSDATE, 'Day DD Month YYYY') Today
   FROM DUAL; 