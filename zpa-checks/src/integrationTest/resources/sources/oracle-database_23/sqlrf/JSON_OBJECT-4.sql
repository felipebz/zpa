-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/JSON_OBJECT.html
SELECT JSON_ARRAYAGG(JSON_OBJECT(*))
FROM departments