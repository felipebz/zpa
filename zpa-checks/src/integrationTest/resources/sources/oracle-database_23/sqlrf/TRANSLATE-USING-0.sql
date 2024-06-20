-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/TRANSLATE-USING.html
CREATE TABLE translate_tab (char_col  VARCHAR2(100),
                            nchar_col NVARCHAR2(50));
INSERT INTO translate_tab 
   SELECT NULL, translated_name
      FROM product_descriptions
      WHERE product_id = 3501;
SELECT * FROM translate_tab;
UPDATE translate_tab 
   SET char_col = TRANSLATE (nchar_col USING CHAR_CS);
SELECT * FROM translate_tab;