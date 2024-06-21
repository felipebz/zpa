-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/SQL_MACRO-clause.html
CREATE FUNCTION date_string(dat DATE) 
                    RETURN VARCHAR2 SQL_MACRO(SCALAR) IS
BEGIN
   RETURN q'{
             TO_CHAR(dat, 'YYYY-MM-DD')
          }';
END;
/