-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json-schema.html
ALTER TABLE employees
  ADD CONSTRAINT max_bonus CHECK ((salary * commission_pct) < 6000);