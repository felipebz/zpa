-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/sql-data-types.html
DECLARE
    TYPE theIBPLS IS TABLE OF NUMBER INDEX BY BINARY_INTEGER;
    myIBPLS theIBPLS;
BEGIN
    myIBPLS := JSON_VALUE(JSON('{"-10":10, "-1":1, "100":-100}'), '$' RETURNING theIBPLS);
END;
/