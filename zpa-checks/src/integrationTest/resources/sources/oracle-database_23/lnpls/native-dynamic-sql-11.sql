-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/native-dynamic-sql.html
CREATE TABLE employees_temp AS SELECT * FROM EMPLOYEES;
DECLARE
  a_null  CHAR(1);  -- Set to NULL automatically at run time
BEGIN
  EXECUTE IMMEDIATE 'UPDATE employees_temp SET commission_pct = :x'
    USING a_null;
END;
/