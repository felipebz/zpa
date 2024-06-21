-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-data-types.html
CREATE PACKAGE myPack IS
    TYPE VECTOR IS TABLE OF BINARY_FLOAT INDEX BY PLS_INTEGER;
END myPack;