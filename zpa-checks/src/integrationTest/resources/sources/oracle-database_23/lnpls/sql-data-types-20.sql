-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/sql-data-types.html
DECLARE
    TYPE theNESTEDTABLE IS TABLE OF NUMBER;
    myNESTEDTABLE theNESTEDTABLE;
BEGIN
    myNESTEDTABLE := JSON_VALUE(JSON('[1, 2, 3, 4, 5]'), '$' RETURNING theNESTEDTABLE);
END;
/