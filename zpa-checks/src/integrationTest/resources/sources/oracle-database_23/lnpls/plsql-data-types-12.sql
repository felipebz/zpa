-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-data-types.html
DECLARE
    TYPE theIBPLS IS TABLE OF NUMBER INDEX BY PLS_INTEGER;
    myIBPLS theIBPLS := theIBPLS(-1=>1, 2=>2, -3=>3);
    myJSON JSON;
BEGIN
    myJSON := JSON(myIBPLS);
    DBMS_OUTPUT.PUT_LINE(JSON_SERIALIZE(myJSON));
END;
/