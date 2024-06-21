-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-data-types.html
DECLARE
    TYPE theIBPLS IS TABLE OF NUMBER INDEX BY PLS_INTEGER;
    myIBPLS theIBPLS;
BEGIN
    myIBPLS := JSON_VALUE(JSON('[1, 2, 3, 4, 5]'), '$' RETURNING theIBPLS);
END;
/