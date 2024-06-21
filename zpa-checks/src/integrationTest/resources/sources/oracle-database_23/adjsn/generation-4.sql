-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/generation.html
CREATE FUNCTION getX(n NUMBER) RETURN VARCHAR2 AS
BEGIN
  RETURN '{"x":'|| n ||'}';
END;