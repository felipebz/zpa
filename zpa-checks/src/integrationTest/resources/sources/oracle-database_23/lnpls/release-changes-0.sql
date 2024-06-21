-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/release-changes.html
CREATE OR REPLACE FUNCTION useBool(p1 BOOLEAN) RETURN NUMBER AS
BEGIN
    IF p1 THEN RETURN 100;
    ELSE
        RETURN 200;
    END IF;
END;
/
SET SERVEROUTPUT ON;
DECLARE
    v1 NUMBER;
    v2 BOOLEAN := TRUE;
BEGIN
    SELECT useBool(v2) INTO v1 FROM dual; --boolean argument function called from SELECT
    DBMS_OUTPUT.PUT_LINE(v1);
END;
/