-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-data-types.html
DECLARE
    TYPE theVARRAY IS VARRAY(5) OF NUMBER;
    myVARRAY theVARRAY;
BEGIN
    myVARRAY := JSON_VALUE(JSON('[1, 2, 3, 4, 5]'), '$' RETURNING theVARRAY);
END;
/