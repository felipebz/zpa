-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/CREATE-FUNCTION-statement.html
CREATE FUNCTION SecondMax (input NUMBER) RETURN NUMBER
    PARALLEL_ENABLE AGGREGATE USING SecondMaxImpl;