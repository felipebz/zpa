-- https://docs.oracle.com/en/database/oracle/oracle-database/26/lnpls/CREATE-FUNCTION-statement.html
CREATE FUNCTION SecondMax (input NUMBER) RETURN NUMBER
    PARALLEL_ENABLE AGGREGATE USING SecondMaxImpl;