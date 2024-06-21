-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/dynamic-sql.html
CREATE OR REPLACE PROCEDURE p AUTHID DEFINER AS
  c1 SYS_REFCURSOR;
  c2 SYS_REFCURSOR;
BEGIN
  OPEN c1 FOR
    SELECT first_name, last_name
    FROM employees
    WHERE employee_id = 176;

  DBMS_SQL.RETURN_RESULT (c1);
  -- Now p cannot access the result.

  OPEN c2 FOR
    SELECT city, state_province
    FROM locations
    WHERE country_id = 'AU';

  DBMS_SQL.RETURN_RESULT (c2);
  -- Now p cannot access the result.
END;
/
BEGIN
  p;
END;
/