-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/NLSSORT.html
ALTER SESSION SET NLS_COMP = 'LINGUISTIC';
ALTER SESSION SET NLS_SORT = 'XDanish';
SELECT *
  FROM test
  WHERE name > 'Gaberd'
  ORDER BY name;