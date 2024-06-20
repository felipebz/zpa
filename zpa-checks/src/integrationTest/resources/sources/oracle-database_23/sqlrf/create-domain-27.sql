-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/create-domain.html
SELECT jt.* FROM user_annotations_usage, 
JSON_TABLE(annotation_value, '$[*]' 
COLUMNS (value VARCHAR2(30 CHAR) PATH '$')) jt 
WHERE annotation_name='DISPLAYUNITS';