-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/sql-data-types.html
DECLARE
    TYPE theNSTTAB IS TABLE OF NUMBER;
    myNSTTAB theNSTTAB;
BEGIN
    myNSTTAB := JSON_VALUE(JSON('{"1":10, "2":20, "3":30, "4":40}'), '$' RETURNING theNSTTAB);
END;
/