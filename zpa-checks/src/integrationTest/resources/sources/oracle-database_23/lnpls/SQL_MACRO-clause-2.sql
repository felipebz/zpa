-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/SQL_MACRO-clause.html
CREATE FUNCTION name_string(first_name VARCHAR2,
                                 last_name VARCHAR2)
                     RETURN VARCHAR2 SQL_MACRO(SCALAR) IS
BEGIN
   RETURN q'{
          TRIM(INITCAP(first_name) || ' ' || INITCAP(last_name))
          }';
END;
/