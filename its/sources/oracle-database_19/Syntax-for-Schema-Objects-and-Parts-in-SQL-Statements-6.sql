-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Syntax-for-Schema-Objects-and-Parts-in-SQL-Statements.html
SELECT TREAT(VALUE(c) AS catalog_typ).getCatalogName() "Catalog Type"
  FROM categories_tab c
  WHERE category_id = 90;