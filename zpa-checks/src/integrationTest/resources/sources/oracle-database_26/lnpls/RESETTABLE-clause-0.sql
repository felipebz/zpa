-- https://docs.oracle.com/en/database/oracle/oracle-database/26/lnpls/RESETTABLE-clause.html
CREATE OR REPLACE PACKAGE res_pkg RESETTABLE AS
    FUNCTION get_current_user
    RETURN VARCHAR2;
END;
/
CREATE OR REPLACE PACKAGE BODY res_pkg RESETTABLE AS 
    
    NAME    VARCHAR2(50);
    ID      PLS_INTEGER;
    MAX_L CONSTANT PLS_INTEGER := ORA_MAX_NAME_LEN;

    FUNCTION get_current_user RETURN VARCHAR2 
    IS
    BEGIN
        RETURN DBMS_ASSERT.enquote_name(SYS_CONTEXT('USERENV', 
                                            'CURRENT_USER'), FALSE);
    END get_current_user;
END;
/