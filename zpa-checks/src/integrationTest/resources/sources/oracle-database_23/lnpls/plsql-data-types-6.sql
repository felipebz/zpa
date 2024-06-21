-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-data-types.html
DECLARE
    TYPE theRec IS RECORD(field1 NUMBER, "Field2" NUMBER);
    myRec theRec := theRec(10, 20);
    myJson JSON;
BEGIN
    myJson := JSON(myRec);
    DBMS_OUTPUT.PUT_LINE(JSON_SERIALIZE(myJson));
END;
/