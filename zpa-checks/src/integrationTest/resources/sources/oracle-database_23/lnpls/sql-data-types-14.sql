-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/sql-data-types.html
DECLARE
    TYPE theNSTTAB IS TABLE OF NUMBER;
    myNSTTAB theNSTTAB := theNSTTAB(1=>1, 2=>2, 3=>3);
    myJSON JSON;
BEGIN
    myNSTTAB.delete(2); --myNSTTAB becomes sparse when elements are deleted
    myJSON := JSON(myNSTTAB);
    DBMS_OUTPUT.PUT_LINE(JSON_SERIALIZE(myJSON));
END;
/