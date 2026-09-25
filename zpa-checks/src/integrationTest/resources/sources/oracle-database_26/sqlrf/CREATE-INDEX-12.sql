-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-INDEX.html
CREATE INDEX upper_ix ON employees (UPPER(last_name));