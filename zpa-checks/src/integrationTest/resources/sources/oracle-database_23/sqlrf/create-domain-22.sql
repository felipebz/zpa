-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/create-domain.html
SELECT jt.* FROM user_annotations_usage a,
  JSON_TABLE (annotation_value,
    '$.allowed_operations.operations[*]'
    COLUMNS (value VARCHAR2(50 CHAR) PATH '$')) jt
  WHERE annotation_name = 'ALLOWED_OPERATIONS'
  AND object_name = 'EMAIL' ;