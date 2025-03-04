-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/sql-data-types.html
DECLARE
    TYPE theRec1 IS RECORD (field1 NUMBER, field2 VARCHAR2(10));
    TYPE theRec2 IS RECORD ("fIeLd2" VARCHAR2(20), "FielD1" NUMBER);

    Rec1 theRec1;
    Rec2 theRec2;
BEGIN
    Rec1 := JSON_VALUE(JSON('{"FIELD1":10, "field2":"hello"}'), '$' RETURNING theRec1);
    Rec2 := JSON_VALUE(JSON('{"FIELD1":10, "field2":"hello"}'), '$' RETURNING theRec2);
END;
/