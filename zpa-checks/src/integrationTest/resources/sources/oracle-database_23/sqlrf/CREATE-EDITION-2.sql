-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-EDITION.html
SELECT SYS_CONTEXT('userenv', 'current_edition_name') FROM DUAL;
CREATE EDITIONING VIEW e_view AS
  SELECT last_name, first_name, email FROM employees;
DESCRIBE e_view
