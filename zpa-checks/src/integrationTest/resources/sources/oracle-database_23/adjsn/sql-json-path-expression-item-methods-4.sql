-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/sql-json-path-expression-item-methods.html
CREATE TABLE tab (data JSON);
INSERT INTO tab VALUES ('{a : [ 1, 2, 3.5 ]}');
SELECT t.data.a[*].sum() from tab t;