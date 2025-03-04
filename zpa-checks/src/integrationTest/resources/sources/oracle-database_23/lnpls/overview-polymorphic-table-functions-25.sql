-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/overview-polymorphic-table-functions.html
WITH t(c1,c2,c3)  AS (
    SELECT NULL, NULL, NULL FROM dual 
    UNION ALL
    SELECT    1, NULL, NULL FROM dual 
    UNION ALL
    SELECT NULL,    2, NULL FROM dual 
    UNION ALL
    SELECT    0, NULL,    3 FROM dual)
  SELECT * 
    FROM to_doc(t);