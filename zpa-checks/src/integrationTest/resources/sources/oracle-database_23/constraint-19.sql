-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/constraint.html
SELECT CONSTRAINT_NAME, SEARCH_CONDITION, PRECHECK 
  FROM USER_CONSTRAINTS 
  WHERE table_name='PRODUCT' and constraint_type='C';