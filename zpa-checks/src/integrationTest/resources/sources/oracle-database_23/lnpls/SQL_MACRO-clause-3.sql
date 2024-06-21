-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/SQL_MACRO-clause.html
CREATE FUNCTION email_string(first_name VARCHAR2,
                                  last_name VARCHAR2,
                                  host_name VARCHAR2 DEFAULT 'example.com')
                    RETURN VARCHAR2 SQL_MACRO(SCALAR) IS
BEGIN
   RETURN q'{
          REPLACE(LOWER(name_string(first_name, last_name)),' ','.') || '@' || host_name
          }';
END;
/