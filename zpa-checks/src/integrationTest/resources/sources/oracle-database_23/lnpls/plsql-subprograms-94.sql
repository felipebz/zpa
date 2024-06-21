-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-subprograms.html
CREATE OR REPLACE FUNCTION get_product_name_2 (
  prod_id NUMBER,
  lang_id VARCHAR2
)
  RETURN NVARCHAR2
  AUTHID DEFINER
IS
  TYPE product_names IS TABLE OF NVARCHAR2(50) INDEX BY PLS_INTEGER;

  FUNCTION all_product_names (lang_id VARCHAR2)
    RETURN product_names
    RESULT_CACHE
  IS
    all_names product_names;
  BEGIN
    FOR c IN (SELECT * FROM OE.Product_Descriptions
              WHERE LANGUAGE_ID = lang_id) LOOP
      all_names(c.PRODUCT_ID) := c.TRANSLATED_NAME;
    END LOOP;
    RETURN all_names;
  END;
BEGIN
  RETURN all_product_names(lang_id)(prod_id);
END;
/