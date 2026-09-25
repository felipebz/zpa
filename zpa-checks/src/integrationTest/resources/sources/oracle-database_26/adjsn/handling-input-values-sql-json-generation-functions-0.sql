-- https://docs.oracle.com/en/database/oracle/oracle-database/26/adjsn/handling-input-values-sql-json-generation-functions.html
CREATE FUNCTION getX(n NUMBER) RETURN VARCHAR2 AS
BEGIN
  RETURN '{"x":'|| n ||'}';
END;