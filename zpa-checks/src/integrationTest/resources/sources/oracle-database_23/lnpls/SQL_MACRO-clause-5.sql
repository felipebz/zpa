-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/SQL_MACRO-clause.html
CREATE FUNCTION emp_xml(first_name VARCHAR2 DEFAULT NULL,
                             last_name VARCHAR2 DEFAULT NULL,
                             hire_date DATE DEFAULT NULL,
                             phone_num VARCHAR2 DEFAULT NULL)
                    RETURN VARCHAR2 SQL_MACRO(SCALAR) IS
BEGIN
   RETURN q'{
       XMLELEMENT("xml",
                  CASE WHEN first_name || last_name IS NOT NULL THEN
                     XMLELEMENT("name", name_string(first_name, last_name))
                  END,
                  CASE WHEN first_name || last_name IS NOT NULL THEN
                     XMLELEMENT("email", email_string(first_name, last_name))
                  END,
                  CASE WHEN hire_date IS NOT NULL THEN
                     XMLELEMENT("hire_date", date_string(hire_date))
                  END,
                  CASE WHEN phone_num IS NOT NULL THEN
                     XMLELEMENT("phone", phone_num)
                  END)
           }';
END;
/