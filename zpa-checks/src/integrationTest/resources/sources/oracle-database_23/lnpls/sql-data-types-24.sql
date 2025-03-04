-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/sql-data-types.html
DECLARE
    TYPE theASCARRAY IS TABLE OF NUMBER INDEX BY VARCHAR2(10);
    myAscArray theASCARRAY;
BEGIN
    myAscArray := JSON_VALUE(JSON('{"Key1":10, "Key2":20}'), '$' RETURNING theASCARRAY);
END;
/