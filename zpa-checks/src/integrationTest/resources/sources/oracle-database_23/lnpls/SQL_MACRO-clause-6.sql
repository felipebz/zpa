-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/SQL_MACRO-clause.html
CREATE FUNCTION emp_doc(first_name VARCHAR2 DEFAULT NULL,
                             last_name VARCHAR2 DEFAULT NULL,
                             hire_date DATE DEFAULT NULL,
                             phone_num VARCHAR2 DEFAULT NULL,
                             doc_type VARCHAR2 DEFAULT 'json')
                    RETURN VARCHAR2 SQL_MACRO(SCALAR) IS
BEGIN
   RETURN q'{
     DECODE(LOWER(doc_type),
            'json', emp_json(first_name, last_name, hire_date, phone_num),
            'xml', emp_xml(first_name, last_name, hire_date, phone_num))
         }';
END;
/