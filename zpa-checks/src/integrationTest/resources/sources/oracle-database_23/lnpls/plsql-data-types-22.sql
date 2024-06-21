-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-data-types.html
DECLARE
    TYPE theVarray IS VARRAY(4) OF NUMBER;
    myVarray theVarray := theVarray(1, 2, 3, null);
    myJSON JSON;
BEGIN
    myJSON := JSON(myVarray);
    DBMS_OUTPUT.PUT_LINE(JSON_SERIALIZE(myJSON));
END;
/