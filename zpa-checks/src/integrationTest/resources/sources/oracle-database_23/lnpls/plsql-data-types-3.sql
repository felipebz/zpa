-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-data-types.html
DECLARE
    TYPE personrecord IS RECORD(first VARCHAR2(10), last VARCHAR2(10));
    p personrecord;
BEGIN
    p := JSON_VALUE(JSON('{"FIRST":"Jane", "LAST":"Cooper"}'), '$'
    RETURNING personrecord USING CASE_SENSITIVE MAPPING);
    DBMS_OUTPUT.PUT_LINE(p.first ||' '|| p.last);
END;
/