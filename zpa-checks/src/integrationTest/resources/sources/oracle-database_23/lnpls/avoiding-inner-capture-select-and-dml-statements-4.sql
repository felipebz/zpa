-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/avoiding-inner-capture-select-and-dml-statements.html
CREATE OR REPLACE TYPE t1 AS OBJECT (x NUMBER);
/
DROP TABLE tb1;
CREATE TABLE tb1 (col1 t1);