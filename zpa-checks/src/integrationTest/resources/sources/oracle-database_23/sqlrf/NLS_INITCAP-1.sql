-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/NLS_INITCAP.html
SELECT NLS_INITCAP('ijsland') "InitCap"
  FROM DUAL;
SELECT NLS_INITCAP('ijsland', 'NLS_SORT = XDutch') "InitCap"
  FROM DUAL;