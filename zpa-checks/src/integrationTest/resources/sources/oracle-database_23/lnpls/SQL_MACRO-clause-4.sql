-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/SQL_MACRO-clause.html
CREATE FUNCTION emp_json(first_name VARCHAR2 DEFAULT NULL,
                              last_name VARCHAR2 DEFAULT NULL,
                              hire_date DATE DEFAULT NULL,
                              phone_num VARCHAR2 DEFAULT NULL)
                    RETURN VARCHAR2 SQL_MACRO(SCALAR) IS
BEGIN
   RETURN q'{
          JSON_OBJECT(
             'name'      : name_string(first_name, last_name),
             'email'     : email_string(first_name, last_name),
             'phone'     : phone_num,
             'hire_date' : date_string(hire_date)
             ABSENT ON NULL)
          }';
END;
/