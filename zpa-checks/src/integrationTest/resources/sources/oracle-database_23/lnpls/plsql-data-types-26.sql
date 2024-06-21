-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-data-types.html
DECLARE
    TYPE AsscArray IS TABLE OF VARCHAR2(10) INDEX BY VARCHAR2(10);
    myAsscArray AsscArray := AsscArray('FIRST_NAME' => 'Bob', 'LAST_NAME' => 'Jones');
    myJson JSON;
BEGIN
    myJson := JSON(myAsscArray);
    DBMS_OUTPUT.PUT_LINE(JSON_SERIALIZE(myJson));
END;
/