-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-subprograms.html
CREATE OR REPLACE FUNCTION get_product_name_1 (
  prod_id NUMBER,
  lang_id VARCHAR2
)
  RETURN NVARCHAR2
  RESULT_CACHE
  AUTHID DEFINER
IS
  result_ VARCHAR2(50);
BEGIN
  SELECT translated_name INTO result_
  FROM OE.Product_Descriptions
  WHERE PRODUCT_ID = prod_id
  AND LANGUAGE_ID = lang_id;
  RETURN result_;
END;
/