-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/generation.html
SELECT json_serialize(data pretty) FROM department_view WHERE id = 90;