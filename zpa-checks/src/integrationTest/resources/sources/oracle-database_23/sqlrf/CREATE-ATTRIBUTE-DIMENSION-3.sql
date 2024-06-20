-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-ATTRIBUTE-DIMENSION.html
CREATE OR REPLACE ATTRIBUTE DIMENSION product_attr_dim
USING product_dim 
ATTRIBUTES
 (department_id,
  department_name,
  category_id,
  category_name)
LEVEL DEPARTMENT
  KEY department_id
  ALTERNATE KEY department_name
  MEMBER NAME department_name
  MEMBER CAPTION department_name
  ORDER BY department_name
LEVEL CATEGORY
  KEY category_id
  ALTERNATE KEY category_name
  MEMBER NAME category_name
  MEMBER CAPTION category_name
  ORDER BY category_name
  DETERMINES(department_id)
ALL MEMBER NAME 'ALL PRODUCTS';