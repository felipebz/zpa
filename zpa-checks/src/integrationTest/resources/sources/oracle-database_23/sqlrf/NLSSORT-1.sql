-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/NLSSORT.html
CREATE TABLE test (name VARCHAR2(15));
INSERT INTO test VALUES ('Gaardiner');
INSERT INTO test VALUES ('Gaberd');
INSERT INTO test VALUES ('Gaasten');
SELECT *
  FROM test
  ORDER BY name;
SELECT *
  FROM test
  ORDER BY NLSSORT(name, 'NLS_SORT = XDanish');